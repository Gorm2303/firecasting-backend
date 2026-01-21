package dk.gormkrings.simulation;

import dk.gormkrings.result.IRunResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class SimulationResultsCache {

    private record Entry(long storedAtMs, List<IRunResult> results) {
    }

    private final ConcurrentHashMap<String, Entry> byId = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<String> order = new ConcurrentLinkedDeque<>();
    private final AtomicReference<String> latestId = new AtomicReference<>(null);

    @Value("${simulation.csv-cache.maxEntries:2}")
    private int maxEntries;

    @Value("${simulation.csv-cache.ttlMs:300000}")
    private long ttlMs;

    public void put(String simulationId, List<IRunResult> results) {
        if (simulationId == null || simulationId.isBlank()) return;
        if (results == null || results.isEmpty()) return;

        long now = System.currentTimeMillis();
        byId.put(simulationId, new Entry(now, results));
        latestId.set(simulationId);

        synchronized (order) {
            // Refresh ordering
            order.remove(simulationId);
            order.addLast(simulationId);
            pruneLocked(now);
        }
    }

    public List<IRunResult> get(String simulationId) {
        if (simulationId == null || simulationId.isBlank()) return null;

        long now = System.currentTimeMillis();
        Entry e = byId.get(simulationId);
        if (e == null) return null;

        if (isExpired(e, now)) {
            remove(simulationId);
            return null;
        }

        // Touch ordering (best-effort)
        synchronized (order) {
            order.remove(simulationId);
            order.addLast(simulationId);
            pruneLocked(now);
        }

        return e.results();
    }

    public List<IRunResult> getLatest() {
        String id = latestId.get();
        return id == null ? null : get(id);
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
        // Drop expired entries from the head
        while (true) {
            String head = order.peekFirst();
            if (head == null) break;
            Entry e = byId.get(head);
            if (e != null && !isExpired(e, now)) break;
            order.pollFirst();
            if (e != null) byId.remove(head);
        }

        // Enforce max size
        while (maxEntries > 0 && order.size() > maxEntries) {
            String oldest = order.pollFirst();
            if (oldest == null) break;
            byId.remove(oldest);
        }
    }
}
