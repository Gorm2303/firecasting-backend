package dk.gormkrings.queue;

import lombok.Value;
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

    private final ThreadPoolExecutor executor;

    // track state per id
    private final ConcurrentHashMap<String, Status> states = new ConcurrentHashMap<>();
    // optional: current running id
    private final AtomicReference<String> runningId = new AtomicReference<>(null);

    public SimulationQueueService(ThreadPoolExecutor executor) {
        this.executor = executor;
    }

    /**
     * Submit a simulation task. Returns true if accepted (queued or started),
     * false if the queue is full (rejected).
     */
    public boolean submit(String simulationId, Runnable task) {
        // wrap to update status transitions
        Runnable wrapped = () -> {
            states.put(simulationId, Status.RUNNING);
            runningId.set(simulationId);
            try {
                task.run();
                states.put(simulationId, Status.DONE);
            } catch (Throwable t) {
                states.put(simulationId, Status.FAILED);
                throw t;
            } finally {
                // clear running marker if it's us
                runningId.compareAndSet(simulationId, null);
            }
        };

        try {
            states.put(simulationId, Status.QUEUED);
            executor.execute(wrapped);
            return true;
        } catch (RejectedExecutionException ex) {
            // queue full
            states.remove(simulationId);
            return false;
        }
    }

    /** Returns current status; null if unknown id. */
    public Status status(String simulationId) {
        return states.get(simulationId);
    }

    /** Returns 0-based position in the queue (0 = next), or -1 if not queued. */
    public int position(String simulationId) {
        // Our queue holds Runnable; we can’t inspect ids unless we wrap.
        // We identify by scanning for our wrapped class and matching by toString().
        // Better: keep a shadow order list.
        int pos = -1;
        int i = 0;
        for (Runnable r : executor.getQueue()) {
            if (r.toString().contains(simulationId)) { // fallback, see submitWithId below if you want strict
                pos = i;
                break;
            }
            i++;
        }
        return pos;
    }

    /** Safer position: keep a shadow queue of ids. Call these on submit and when task starts. */
    private final ConcurrentLinkedQueue<String> shadowQueue = new ConcurrentLinkedQueue<>();

    public boolean submitWithId(String simulationId, Runnable task) {
        Runnable wrapped = () -> {
            // on start: pop from shadow queue head if it’s us
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
            states.remove(simulationId);
            shadowQueue.remove(simulationId);
            return false;
        }
    }

    /** Position using shadow queue (preferred). 0 = next, -1 = not queued. */
    public int queuedPosition(String simulationId) {
        int idx = 0;
        for (String id : shadowQueue) {
            if (Objects.equals(id, simulationId)) return idx;
            idx++;
        }
        return -1;
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
