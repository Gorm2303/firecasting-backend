package dk.gormkrings.simulation;

import dk.gormkrings.statistics.MetricSummary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class SimulationMetricSummariesCache {

    private record Entry(long storedAtMs, List<MetricSummary> summaries) {
    }

    private final ConcurrentHashMap<String, Entry> byId = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<String> order = new ConcurrentLinkedDeque<>();
    private final AtomicReference<String> latestId = new AtomicReference<>(null);

    @Value("${simulation.metric-summaries-cache.maxEntries:50}")
    private int maxEntries;

    @Value("${simulation.metric-summaries-cache.ttlMs:900000}")
    private long ttlMs;

    public void put(String simulationId, List<MetricSummary> summaries) {
        if (simulationId == null || simulationId.isBlank()) return;
        if (summaries == null || summaries.isEmpty()) return;

        long now = System.currentTimeMillis();
        byId.put(simulationId, new Entry(now, summaries));
        latestId.set(simulationId);

        synchronized (order) {
            order.remove(simulationId);
            order.addLast(simulationId);
            pruneLocked(now);
        }
    }

    public List<MetricSummary> get(String simulationId) {
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

        return e.summaries();
    }

    public List<MetricSummary> getLatest() {
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
