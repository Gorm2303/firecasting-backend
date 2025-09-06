package dk.gormkrings.queue;

import lombok.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class SimulationQueueService {

    public enum Status { QUEUED, RUNNING, DONE, FAILED }

    @Value
    public static class TaskInfo {
        String simulationId;
        Status status;
        Integer position; // null if not queued
    }

    /** Executor wired from ExecConfig (bean name: simExecutor). */
    private final ExecutorService executor;

    // track state per id
    private final ConcurrentHashMap<String, Status> states = new ConcurrentHashMap<>();
    // optional: current running id
    private final AtomicReference<String> runningId = new AtomicReference<>(null);
    // keep a shadow FIFO of queued ids to compute positions
    private final ConcurrentLinkedQueue<String> shadowQueue = new ConcurrentLinkedQueue<>();

    public SimulationQueueService(@Qualifier("simExecutor") ExecutorService executor) {
        this.executor = executor;
    }

    /** Convenience: delegates to submitWithId (preferred). */
    public boolean submit(String simulationId, Runnable task) {
        return submitWithId(simulationId, task);
    }

    /**
     * Submit a simulation task. Returns true if accepted (queued or started),
     * false if the queue is full (rejected).
     */
    public boolean submitWithId(String simulationId, Runnable task) {
        Runnable wrapped = () -> {
            // on start: pop from shadow queue head
            shadowQueue.poll();
            states.put(simulationId, Status.RUNNING);
            runningId.set(simulationId);
            try {
                task.run();
                states.put(simulationId, Status.DONE);
            } catch (Throwable t) {
                states.put(simulationId, Status.FAILED);
                throw t;
            } finally {
                runningId.compareAndSet(simulationId, null);
            }
        };

        try {
            states.put(simulationId, Status.QUEUED);
            shadowQueue.add(simulationId);
            executor.execute(wrapped);
            return true;
        } catch (RejectedExecutionException ex) {
            // queue full
            states.remove(simulationId);
            shadowQueue.remove(simulationId);
            return false;
        }
    }

    /** Returns current status; null if unknown id. */
    public Status status(String simulationId) {
        return states.get(simulationId);
    }

    /** Position using shadow queue (0 = next, -1 = not queued). */
    public int queuedPosition(String simulationId) {
        int idx = 0;
        for (String id : shadowQueue) {
            if (Objects.equals(id, simulationId)) return idx;
            idx++;
        }
        return -1;
    }

    /** Backward-compatible alias for position; uses shadow queue. */
    public int position(String simulationId) {
        return queuedPosition(simulationId);
    }

    public TaskInfo info(String simulationId) {
        Status s = status(simulationId);
        Integer pos = null;
        if (s == Status.QUEUED) {
            int p = queuedPosition(simulationId);
            pos = (p >= 0) ? p : null;
        }
        return new TaskInfo(simulationId, s, pos);
    }
}
