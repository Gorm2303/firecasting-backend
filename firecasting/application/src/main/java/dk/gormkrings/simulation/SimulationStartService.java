package dk.gormkrings.simulation;

import dk.gormkrings.queue.SimulationQueueService;
import dk.gormkrings.sse.SimulationSseService;
import dk.gormkrings.statistics.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimulationStartService {

    private final SimulationQueueService simQueue;
    private final SimulationRunner simulationRunner;
    private final SimulationSseService sseService;
    private final StatisticsService statisticsService;
    private final SimulationResultsCache resultsCache;

    @Value("${settings.runs}")
    private int runs;

    @Value("${settings.batch-size}")
    private int batchSize;

    @FunctionalInterface
    public interface SimulationPostProcessor {
        /**
         * Runs inside the simulation queue job after persistence has completed, but before SSE "completed" is sent.
         */
        void afterRun(String simulationId);
    }

    /**
     * Shared start logic used by multiple endpoints.
     *
     * @param inputForDedupAndStorage the object that should be serialized for dedup hashing and persisted
     *                                as "inputJson". For the legacy endpoint this must be the legacy
     *                                request DTO to preserve behavior.
     */
    public ResponseEntity<Map<String, String>> startSimulation(
            String logPrefix,
            SimulationRunSpec spec,
            Object inputForDedupAndStorage) {

        return startSimulation(logPrefix, spec, inputForDedupAndStorage, null);
        }

        /**
         * Like {@link #startSimulation(String, SimulationRunSpec, Object)} but allows a post-processing hook
         * to run after persistence.
         */
        public ResponseEntity<Map<String, String>> startSimulation(
            String logPrefix,
            SimulationRunSpec spec,
            Object inputForDedupAndStorage,
            SimulationPostProcessor postProcessor) {

            // Negative seeds are explicitly treated as "stochastic" runs.
            // To ensure repeating a run with the same negative seed still yields a new outcome,
            // we must not deduplicate such requests.
            Long rngSeed = (spec.getReturnerConfig() == null) ? null : spec.getReturnerConfig().getSeed();
            boolean allowDedup = (rngSeed == null || rngSeed >= 0);

            // 0) Dedup FIRST (unless stochastic seed), return immediately if hit
            if (allowDedup) {
                try {
                    Optional<String> existingId = statisticsService.findExistingRunIdForInput(inputForDedupAndStorage);
                    if (existingId.isPresent()) {
                        log.info("[{}] Dedup hit -> {}", logPrefix, existingId.get());
                        return ResponseEntity.ok(Map.of("id", existingId.get()));
                    }
                    log.info("[{}] Dedup miss -> creating new run", logPrefix);
                } catch (Exception e) {
                    log.error("[{}] Dedup check failed", logPrefix, e);
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Invalid input for dedup: " + e.getMessage()));
                }
            } else {
                log.info("[{}] Skipping dedup due to stochastic seed ({})", logPrefix, rngSeed);
            }

        // 1) Simple invariant: max duration
        int totalMonths = spec.getTotalMonths();
        if (totalMonths > 1200) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "Total duration across phases must be ≤ 1200 months (got " + totalMonths + ")"));
        }

        // 2) New run id, enqueue job, respond 202 with JSON {id}
        final String simulationId = UUID.randomUUID().toString();
        log.info("[{}] New run -> {}", logPrefix, simulationId);

        boolean accepted = simQueue.submitWithId(simulationId, () -> {
            try {
                // Start SSE flushing pipeline
                sseService.startFlusher(simulationId);

                var results = simulationRunner.runSimulation(
                        simulationId,
                        spec,
                        inputForDedupAndStorage,
                        runs,
                        batchSize,
                        msg -> sseService.onProgressMessage(simulationId, msg)
                );

                // Cache full run results briefly to support deterministic per-run CSV export.
                // This is intentionally in-memory only.
                try {
                    resultsCache.put(simulationId, results);
                } catch (Exception e) {
                    // Don't fail the run if caching fails; CSV export will simply be unavailable.
                    log.warn("[{}] Failed to cache full results for {}", logPrefix, simulationId, e);
                }

                if (postProcessor != null) {
                    try {
                        postProcessor.afterRun(simulationId);
                    } catch (Exception e) {
                        log.error("[{}] Post-processing failed for {}", logPrefix, simulationId, e);
                    }
                }

                // Fetch summaries from DB and emit "completed" with data
                var summaries = statisticsService.getSummariesForRun(simulationId);
                sseService.sendCompleted(simulationId, summaries);
            } catch (Exception e) {
                log.error("Simulation failed {}", simulationId, e);
                sseService.stopFlusherWithError(simulationId, e);
            } finally {
                sseService.stopFlusherAndComplete(simulationId);
            }
        });

        if (!accepted) {
            return ResponseEntity.status(429)
                    .body(Map.of("error", "Queue full. Try again later."));
        }

        sseService.enqueueStateQueued(simulationId, "queued");
        return ResponseEntity.accepted().body(Map.of("id", simulationId));
    }
}
