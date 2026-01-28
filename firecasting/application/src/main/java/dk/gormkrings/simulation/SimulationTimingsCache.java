package dk.gormkrings.simulation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class SimulationTimingsCache {

    public record Timings(long queueMs,
                          long computeMs,
                          long aggregateMs,
                          long gridsMs,
                          long persistMs,
                          long totalMs) {
    }

    private record Entry(long storedAtMs, Map<String, Object> meta, Timings timings) {
    }

    private final ConcurrentHashMap<String, Entry> byId = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<String> order = new ConcurrentLinkedDeque<>();
    private final AtomicReference<String> latestId = new AtomicReference<>(null);

    @Value("${simulation.timings-cache.maxEntries:200}")
    private int maxEntries;

    @Value("${simulation.timings-cache.ttlMs:900000}")
    private long ttlMs;

    public void put(String simulationId, Map<String, Object> meta, Timings timings) {
        if (simulationId == null || simulationId.isBlank()) return;
        if (meta == null || meta.isEmpty()) return;

        long now = System.currentTimeMillis();
        byId.put(simulationId, new Entry(now, meta, timings));
        latestId.set(simulationId);

        synchronized (order) {
            order.remove(simulationId);
            order.addLast(simulationId);
            pruneLocked(now);
        }
    }

    public Map<String, Object> getMeta(String simulationId) {
        Entry e = getEntry(simulationId);
        return e == null ? null : e.meta();
    }

    public Timings getTimings(String simulationId) {
        Entry e = getEntry(simulationId);
        return e == null ? null : e.timings();
    }

    public Map<String, Object> getLatestMeta() {
        String id = latestId.get();
        return id == null ? null : getMeta(id);
    }

    private Entry getEntry(String simulationId) {
        if (simulationId == null || simulationId.isBlank()) return null;

        long now = System.currentTimeMillis();
        Entry e = byId.get(simulationId);
        if (e == null) return null;

        if (isExpired(e, now)) {
            remove(simulationId);
            return null;
        }

        synchronized (order) {
            order.remove(simulationId);
            order.addLast(simulationId);
            pruneLocked(now);
        }

        return e;
    }

    private boolean isExpired(Entry e, long now) {
        return ttlMs > 0 && (now - e.storedAtMs()) > ttlMs;
    }

    private void remove(String simulationId) {
        byId.remove(simulationId);
        synchronized (order) {
            order.remove(simulationId);
        }
    }

    private void pruneLocked(long now) {
        while (true) {
            String head = order.peekFirst();
            if (head == null) break;
            Entry e = byId.get(head);
            if (e != null && !isExpired(e, now)) break;
            order.pollFirst();
            if (e != null) byId.remove(head);
        }

        while (maxEntries > 0 && order.size() > maxEntries) {
            String oldest = order.pollFirst();
            if (oldest == null) break;
            byId.remove(oldest);
        }
    }
}
