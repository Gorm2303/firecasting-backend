package dk.gormkrings.queue;

import lombok.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.*;

@Service
public class SimulationQueueService {

    public enum Status { QUEUED, RUNNING, DONE, FAILED }

    @Value
    public static class TaskInfo {
        String simulationId;
        Status status;
        Integer position; // 1-based when QUEUED; null otherwise
    }

    /** Executor wired from ExecConfig (bean name: simExecutor). */
    private final ExecutorService executor;

    // status per id
    private final ConcurrentHashMap<String, Status> states = new ConcurrentHashMap<>();
    // FIFO of queued ids to compute positions (order of arrival)
    private final ConcurrentLinkedQueue<String> shadowQueue = new ConcurrentLinkedQueue<>();

    public SimulationQueueService(@Qualifier("simExecutor") ExecutorService executor) {
        this.executor = executor;
    }

    /** Convenience: delegates to submitWithId (preferred). */
    public boolean submit(String simulationId, Runnable task) {
        return submitWithId(simulationId, task);
    }

    /**
     * Submit a simulation task with a stable id.
     * Returns true if accepted (queued or already known), false if the executor rejects it.
     * Idempotent: if the id is already QUEUED/RUNNING/DONE/FAILED, we don't enqueue again.
     */
    public boolean submitWithId(String simulationId, Runnable task) {
        Objects.requireNonNull(simulationId, "simulationId");
        Objects.requireNonNull(task, "task");

        // idempotent: if we have a record for this id, don't add it again
        Status existing = states.putIfAbsent(simulationId, Status.QUEUED);
        if (existing != null) {
            // Already known (QUEUED/RUNNING/DONE/FAILED). Treat as accepted.
            return true;
        }

        // track arrival order for position computation
        shadowQueue.add(simulationId);

        Runnable wrapped = () -> {
            // When this task actually starts, remove *this id* (not just queue head)
            shadowQueue.remove(simulationId);
            states.put(simulationId, Status.RUNNING);
            try {
                task.run();
                states.put(simulationId, Status.DONE);
            } catch (Throwable t) {
                states.put(simulationId, Status.FAILED);
                throw t;
            }
        };

        try {
            executor.execute(wrapped);
            return true;
        } catch (RejectedExecutionException ex) {
            // rollback if executor queue is full
            states.remove(simulationId);
            shadowQueue.remove(simulationId);
            return false;
        }
    }

    /** Returns current status; null if unknown id. */
    public Status status(String simulationId) {
        return states.get(simulationId);
    }

    /**
     * 1-based position in the waiting queue (1 = next to run).
     * Returns -1 if not currently queued.
     */
    public int queuedPosition(String simulationId) {
        int idx = 0;
        for (String id : shadowQueue) {
            idx++;
            if (Objects.equals(id, simulationId)) return idx;
        }
        return -1;
    }

    /** Backward-compatible alias for position (1-based). */
    public int position(String simulationId) {
        return queuedPosition(simulationId);
    }

    /** Combined view for SSE/HTTP APIs. */
    public TaskInfo info(String simulationId) {
        Status s = status(simulationId);
        if (s == null) return null;

        Integer pos = null;
        if (s == Status.QUEUED) {
            int p = queuedPosition(simulationId);
            pos = (p > 0) ? p : null; // 1-based
        }
        return new TaskInfo(simulationId, s, pos);
    }
}
