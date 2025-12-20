package dk.gormkrings.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.gormkrings.dto.ProgressUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class SimulationSseService {

    private final ScheduledExecutorService sseScheduler;
    private final ObjectMapper mapper;

    public SimulationSseService(ScheduledExecutorService sseScheduler, ObjectMapper mapper) {
        this.sseScheduler = sseScheduler;
        this.mapper = mapper;
    }

    @Value("${settings.sse-interval:1000}")
    private long sseIntervalMs;

    // ---- priority and internal state ---------------------------------------

    private static final int PRIO_HEARTBEAT = -1;
    private static final int PRIO_PROGRESS  =  0;
    private static final int PRIO_OTHER     =  1;
    private static final int PRIO_STATE     =  2;

    private static final class PendingEvent {
        final String name;
        final Object data;
        final MediaType type;
        final int priority;
        PendingEvent(String name, Object data, MediaType type, int priority) {
            this.name = name; this.data = data; this.type = type; this.priority = priority;
        }
    }

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicReference<PendingEvent>> pendingBySim = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ScheduledFuture<?>> flusherBySim = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> lastStateSent = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> lastQueuedPos = new ConcurrentHashMap<>();

    // ---- public API ---------------------------------------------------------

    public void addEmitter(String simId, SseEmitter emitter) {
        emitters.computeIfAbsent(simId, __ -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(simId, emitter));
        emitter.onTimeout(() -> removeEmitter(simId, emitter));
    }

    public void sendCompleted(String simId, Object data) {
        flushOnce(simId);
        broadcast(simId, SseEmitter.event().name("completed")
                .data(data, MediaType.APPLICATION_JSON));
    }

    public void onProgressMessage(String simId, String progressMessage) {
        if (progressMessage == null) return;
        String msg = progressMessage.trim();
        boolean handled = false;

        if (msg.startsWith("{") && msg.contains("\"kind\"")) {
            try {
                ProgressUpdate pu = mapper.readValue(msg, ProgressUpdate.class);
                if (pu.getKind() == ProgressUpdate.Kind.RUNS) {
                    String line = String.format("Completed %,d/%,d runs", pu.getCompleted(), pu.getTotal());
                    enqueue(simId, "progress", line, PRIO_PROGRESS);
                    handled = true;
                } else if (pu.getKind() == ProgressUpdate.Kind.MESSAGE && pu.getMessage() != null) {
                    enqueue(simId, "progress", pu.getMessage(), PRIO_OTHER);
                    handled = true;
                }
            } catch (Exception ignore) {
                // fall back
            }
        }
        if (!handled) {
            enqueue(simId, "progress", msg, PRIO_OTHER);
        }
    }

    public void enqueueStateQueued(String simId, String payload) {
        enqueue(simId, "queued", payload, PRIO_STATE);
    }

    public void enqueueStateStarted(String simId, String payload) {
        enqueue(simId, "started", payload, PRIO_STATE);
    }

    public void enqueueHeartbeat(String simId) {
        enqueue(simId, "heartbeat", "tick", PRIO_HEARTBEAT);
    }

    public void startFlusher(String simId) {
        flusherBySim.computeIfAbsent(simId, id ->
                sseScheduler.scheduleAtFixedRate(() -> flushOnce(id), 0L, sseIntervalMs, TimeUnit.MILLISECONDS)
        );
    }

    public void stopFlusherAndComplete(String simId) {
        stopFlusher(simId);
        var list = emitters.remove(simId);
        if (list != null) {
            for (var em : list) {
                try { em.complete(); } catch (Exception ignore) {}
            }
        }
    }

    public void stopFlusherWithError(String simId, Throwable t) {
        stopFlusher(simId);
        var list = emitters.remove(simId);
        if (list != null) {
            for (var em : list) {
                try { em.completeWithError(t); } catch (Exception ignore) {}
            }
        }
    }

    public String getLastState(String simId) {
        return lastStateSent.get(simId);
    }

    public void setLastState(String simId, String state) {
        lastStateSent.put(simId, state);
    }

    public Integer getLastQueuedPos(String simId) {
        return lastQueuedPos.get(simId);
    }

    public void setLastQueuedPos(String simId, Integer pos) {
        if (pos == null) lastQueuedPos.remove(simId);
        else lastQueuedPos.put(simId, pos);
    }

    public void clearState(String simId) {
        lastStateSent.remove(simId);
        lastQueuedPos.remove(simId);
    }

    // ---- internal helpers ---------------------------------------------------

    private void removeEmitter(String id, SseEmitter e) {
        var list = emitters.get(id);
        if (list != null) list.remove(e);
    }

    private void broadcast(String id, SseEmitter.SseEventBuilder event) {
        var list = emitters.get(id);
        if (list == null || list.isEmpty()) return;

        List<SseEmitter> dead = null;
        for (var e : list) {
            try { e.send(event); }
            catch (Exception ex) {
                if (dead == null) dead = new ArrayList<>();
                dead.add(e);
            }
        }
        if (dead != null) list.removeAll(dead);
    }

    private void enqueue(String simId, String name, Object data, int prio) {
        var ref = pendingBySim.computeIfAbsent(simId, __ -> new AtomicReference<>());
        PendingEvent next = new PendingEvent(name, data, MediaType.TEXT_PLAIN, prio);

        while (true) {
            PendingEvent cur = ref.get();
            if (cur == null) {
                if (ref.compareAndSet(null, next)) break;
            } else {
                if (prio < cur.priority) break; // lower prio → ignore
                if (ref.compareAndSet(cur, next)) break;
            }
        }
        startFlusher(simId);
    }

    private void stopFlusher(String simId) {
        var f = flusherBySim.remove(simId);
        if (f != null) f.cancel(false);
        pendingBySim.remove(simId);
        clearState(simId);
    }

    private void flushOnce(String simId) {
        var ref = pendingBySim.get(simId);
        if (ref == null) return;
        PendingEvent ev = ref.getAndSet(null);
        if (ev == null) return;

        var sse = SseEmitter.event().name(ev.name);
        if (ev.data != null) sse = sse.data(ev.data, ev.type);
        broadcast(simId, sse);
    }
}

