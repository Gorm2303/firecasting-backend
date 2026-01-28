package dk.gormkrings.simulation;

import dk.gormkrings.queue.SimulationQueueService;
import dk.gormkrings.sse.SimulationSseService;
import dk.gormkrings.statistics.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimulationStartService {

    private static final int MAX_PATHS = 100_000;
    private static final int MAX_BATCH_SIZE = 100_000;

    private final SimulationQueueService simQueue;
    private final SimulationRunner simulationRunner;
    private final SimulationSseService sseService;
    private final StatisticsService statisticsService;
    private final SimulationResultsCache resultsCache;
    private final SimulationSummariesCache summariesCache;
    private final SimulationMetricSummariesCache metricSummariesCache;
    private final SimulationTimingsCache timingsCache;

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
     * Shared start logic used by both /start and /start-advanced endpoints.
     *
     * @param inputForDedupAndStorage the object that should be serialized for dedup hashing and persisted
     *                                as "inputJson". For legacy dedup purposes.
     * @param resolvedAdvanced the fully resolved AdvancedSimulationRequest with all defaults applied,
     *                          will be persisted as resolvedInputJson for frontend transparency.
     */
    public ResponseEntity<Map<String, String>> startSimulation(
            String logPrefix,
            SimulationRunSpec spec,
            Object inputForDedupAndStorage,
            Object resolvedAdvanced) {

        return startSimulation(
            logPrefix,
            spec,
            inputForDedupAndStorage,
            resolvedAdvanced,
            null,
            inferRequestedSeedForDedup(inputForDedupAndStorage, resolvedAdvanced)
        );
    }

    /**
     * Legacy overload used by older tests and code paths that only provide the
     * input for dedup/storage. Internally delegates to the newer signature with
     * a null resolved advanced request.
     */
    public ResponseEntity<Map<String, String>> startSimulation(
            String logPrefix,
            SimulationRunSpec spec,
            Object inputForDedupAndStorage) {

        return startSimulation(
                logPrefix,
                spec,
                inputForDedupAndStorage,
                null,
                null,
                inferRequestedSeedForDedup(inputForDedupAndStorage, null)
        );
    }

    private static Long inferRequestedSeedForDedup(Object inputForDedupAndStorage, Object resolvedAdvanced) {
        if (inputForDedupAndStorage instanceof dk.gormkrings.dto.AdvancedSimulationRequest a) {
            return a.getSeed();
        }
        if (inputForDedupAndStorage instanceof dk.gormkrings.dto.SimulationRequest n) {
            return n.getSeed();
        }
        if (resolvedAdvanced instanceof dk.gormkrings.dto.AdvancedSimulationRequest a) {
            return a.getSeed();
        }
        return null;
    }

    /**
     * Like {@link #startSimulation(String, SimulationRunSpec, Object, Object)} but allows a post-processing hook
     * to run after persistence.
     */
    public ResponseEntity<Map<String, String>> startSimulation(
            String logPrefix,
            SimulationRunSpec spec,
            Object inputForDedupAndStorage,
            Object resolvedAdvanced,
            SimulationPostProcessor postProcessor,
            Long requestedSeedForDedup) {

            final int effectiveRuns = resolvePathsOverride(resolvedAdvanced, inputForDedupAndStorage);
            final int effectiveBatchSize = resolveBatchSizeOverride(resolvedAdvanced, inputForDedupAndStorage);

            // Contract:
            // - requestedSeed < 0 => RANDOM run (stochastic): never dedup and never persist in DB.
            // - requestedSeed == null or >= 0 => deterministic run: dedup+persist.
            // IMPORTANT: base this on the *requested* seed, not the normalized execution seed.
            final boolean isRandomRequested = (requestedSeedForDedup != null && requestedSeedForDedup < 0);
            final boolean allowDedup = !isRandomRequested;
            final boolean allowPersist = !isRandomRequested;

            // 0) Dedup FIRST (unless stochastic seed), return immediately if hit
            if (allowDedup) {
                try {
                    Optional<String> existingId = statisticsService.findExistingRunIdForSignature(
                            SimulationSignature.of(effectiveRuns, effectiveBatchSize, inputForDedupAndStorage)
                    );

                    // Backwards compatibility: older runs used a signature without batchSize.
                    // Only fall back when the request uses the default batch size.
                    if (existingId.isEmpty() && effectiveBatchSize == batchSize) {
                        existingId = statisticsService.findExistingRunIdForSignature(
                                new LegacySimulationSignature(effectiveRuns, inputForDedupAndStorage)
                        );
                    }

                    if (existingId.isPresent()) {
                        log.info("[{}] Dedup hit -> {}", logPrefix, existingId.get());

                        // Best-effort: attach persisted metadata so the frontend can fill diff tables.
                        var run = statisticsService.getRun(existingId.get());
                        if (run != null) {
                            return ResponseEntity.ok(Map.of(
                                    "id", existingId.get(),
                                    "dedupHit", "true",
                                "effectiveRuns", String.valueOf(effectiveRuns),
                                "effectiveBatchSize", String.valueOf(effectiveBatchSize),
                                    "createdAt", run.getCreatedAt() != null ? run.getCreatedAt().toString() : "",
                                    "rngSeed", run.getRngSeed() != null ? run.getRngSeed().toString() : "",
                                    "modelAppVersion", run.getModelAppVersion() != null ? run.getModelAppVersion() : "",
                                    "modelBuildTime", run.getModelBuildTime() != null ? run.getModelBuildTime() : "",
                                    "modelSpringBootVersion", run.getModelSpringBootVersion() != null ? run.getModelSpringBootVersion() : "",
                                    "modelJavaVersion", run.getModelJavaVersion() != null ? run.getModelJavaVersion() : ""
                            ));
                        }

                        return ResponseEntity.ok(Map.of(
                            "id", existingId.get(),
                            "dedupHit", "true",
                            "effectiveRuns", String.valueOf(effectiveRuns),
                            "effectiveBatchSize", String.valueOf(effectiveBatchSize)
                        ));
                    }
                    log.info("[{}] Dedup miss -> creating new run", logPrefix);
                } catch (Exception e) {
                    // Dedup failures can be caused by DB anomalies (e.g. historical duplicates).
                    // Do not fail the request; proceed as a dedup miss.
                    log.warn("[{}] Dedup check failed (treating as miss): {}", logPrefix, e.getMessage());
                }
            } else {
                log.info("[{}] Skipping dedup due to random seed request ({})", logPrefix, requestedSeedForDedup);
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

        final long enqueuedAtMs = System.currentTimeMillis();

        boolean accepted = simQueue.submitWithId(simulationId, () -> {
            try {
                // Start SSE flushing pipeline
                sseService.startFlusher(simulationId);

            final long dequeuedAtMs = System.currentTimeMillis();
            final long queueMs = Math.max(0L, dequeuedAtMs - enqueuedAtMs);

                var outcome = simulationRunner.runSimulationOutcome(
                    simulationId,
                    spec,
                    inputForDedupAndStorage,
                    resolvedAdvanced,
                    effectiveRuns,
                    effectiveBatchSize,
                    allowPersist,
                    msg -> sseService.onProgressMessage(simulationId, msg)
                );

                // Emit final timing breakdown before completion payload.
                try {
                    var t = outcome.timings();
                    long computeMs = t != null ? t.computeMs() : 0;
                    long aggregateMs = t != null ? t.aggregateMs() : 0;
                    long gridsMs = t != null ? t.gridsMs() : 0;
                    long persistMs = t != null ? t.persistMs() : 0;
                    long totalMs = t != null ? t.totalMs() : (computeMs + aggregateMs + gridsMs + persistMs);

                    var meta = new HashMap<String, Object>();
                    meta.put("dedupHit", false);
                    meta.put("persisted", allowPersist);
                    meta.put("effectiveRuns", effectiveRuns);
                    meta.put("effectiveBatchSize", effectiveBatchSize);
                    meta.put("queueMs", queueMs);
                    meta.put("computeMs", computeMs);
                    meta.put("aggregateMs", aggregateMs);
                    meta.put("gridsMs", gridsMs);
                    meta.put("persistMs", persistMs);
                    meta.put("totalMs", totalMs);

                    timingsCache.put(
                            simulationId,
                            Map.copyOf(meta),
                            new SimulationTimingsCache.Timings(queueMs, computeMs, aggregateMs, gridsMs, persistMs, totalMs)
                    );

                    sseService.sendMeta(simulationId, meta);
                } catch (Exception ignore) {
                    // best-effort
                }

                // Cache full run results briefly to support deterministic per-run CSV export.
                // This is intentionally in-memory only.
                try {
                    resultsCache.put(simulationId, outcome.results());
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

                if (allowPersist) {
                    // Fetch summaries from DB and emit "completed" with data
                    var summaries = statisticsService.getSummariesForRun(simulationId);
                    sseService.sendCompleted(simulationId, summaries);
                } else {
                    // Random runs are intentionally not persisted. Cache summaries in-memory
                    // so /progress/{id} JSON and SSE can still return completed data.
                    summariesCache.put(simulationId, outcome.summaries());
                    metricSummariesCache.put(simulationId, outcome.metricSummaries());
                    sseService.sendCompleted(simulationId, outcome.summaries());
                }
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

        final var mv = statisticsService.getModelVersionInfo();
        final Long rngSeed = (spec.getReturnerConfig() == null) ? null : spec.getReturnerConfig().getSeed();

        return ResponseEntity.accepted().body(Map.of(
            "id", simulationId,
            "dedupHit", "false",
            "effectiveRuns", String.valueOf(effectiveRuns),
            "effectiveBatchSize", String.valueOf(effectiveBatchSize),
            "createdAt", OffsetDateTime.now().toString(),
            "rngSeed", rngSeed != null ? rngSeed.toString() : "",
            "modelAppVersion", mv != null && mv.modelAppVersion() != null ? mv.modelAppVersion() : "",
            "modelBuildTime", mv != null && mv.modelBuildTime() != null ? mv.modelBuildTime() : "",
            "modelSpringBootVersion", mv != null && mv.modelSpringBootVersion() != null ? mv.modelSpringBootVersion() : "",
            "modelJavaVersion", mv != null && mv.modelJavaVersion() != null ? mv.modelJavaVersion() : ""
        ));
    }

    private int resolvePathsOverride(Object resolvedAdvanced, Object inputForDedupAndStorage) {
        Integer requested = null;

        if (resolvedAdvanced instanceof dk.gormkrings.dto.AdvancedSimulationRequest a) {
            requested = a.getPaths();
        } else if (inputForDedupAndStorage instanceof dk.gormkrings.dto.AdvancedSimulationRequest a) {
            requested = a.getPaths();
        }

        if (requested == null) return runs;
        if (requested <= 0) return runs;
        return Math.min(requested, MAX_PATHS);
    }

    private int resolveBatchSizeOverride(Object resolvedAdvanced, Object inputForDedupAndStorage) {
        Integer requested = null;

        if (resolvedAdvanced instanceof dk.gormkrings.dto.AdvancedSimulationRequest a) {
            requested = a.getBatchSize();
        } else if (inputForDedupAndStorage instanceof dk.gormkrings.dto.AdvancedSimulationRequest a) {
            requested = a.getBatchSize();
        }

        if (requested == null) return batchSize;
        if (requested <= 0) return batchSize;
        return Math.min(requested, MAX_BATCH_SIZE);
    }

    /**
     * Legacy signature payload used before batchSize participated in the signature.
     * Kept only for backwards-compatible run lookups/dedup.
     */
    private record LegacySimulationSignature(int paths, Object input) {
    }

    /**
     * Legacy overload used by tests that provide a post-processor but no resolved advanced input.
     */
    public ResponseEntity<Map<String, String>> startSimulation(
            String logPrefix,
            SimulationRunSpec spec,
            Object inputForDedupAndStorage,
            SimulationPostProcessor postProcessor) {

        return startSimulation(
                logPrefix,
                spec,
                inputForDedupAndStorage,
                null,
                postProcessor,
                inferRequestedSeedForDedup(inputForDedupAndStorage, null)
        );
    }
}
