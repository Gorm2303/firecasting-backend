package dk.gormkrings;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.gormkrings.dto.PhaseRequest;
import dk.gormkrings.dto.AdvancedSimulationRequest;
import dk.gormkrings.dto.SimulationRequest;
import dk.gormkrings.api.ApiValidationException;
import dk.gormkrings.queue.SimulationQueueService;
import dk.gormkrings.result.IRunResult;
import dk.gormkrings.simulation.AdvancedSimulationRequestMapper;
import dk.gormkrings.simulation.SimulationRunSpec;
import dk.gormkrings.simulation.SimulationStartService;
import dk.gormkrings.simulation.SimulationResultsCache;
import dk.gormkrings.simulation.SimulationSummariesCache;
import dk.gormkrings.simulation.SimulationRunner;
import dk.gormkrings.simulation.util.ConcurrentCsvExporter;
import dk.gormkrings.statistics.StatisticsService;
import dk.gormkrings.statistics.YearlySummary;
import dk.gormkrings.sse.SimulationSseService;
import dk.gormkrings.ui.fields.UISchemaField;
import dk.gormkrings.ui.generator.UISchemaGenerator;
import dk.gormkrings.export.ReproducibilityBundleService;
import dk.gormkrings.export.ReproducibilityBundleDto;
import dk.gormkrings.diff.RunDiffService;
import dk.gormkrings.diff.RunDiffResponse;
import dk.gormkrings.diff.RunListItemDto;
import dk.gormkrings.reproducibility.ReplayStartResponse;
import dk.gormkrings.reproducibility.ReplayStatusResponse;
import dk.gormkrings.reproducibility.ReproducibilityReplayService;
import dk.gormkrings.returns.ReturnerConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Profile("!local") // only active when NOT in local mode
@RestController
@RequestMapping("/api/simulation")
public class FirecastingController {

    private final SimulationQueueService simQueue;
    private final SimulationSseService sseService;
    private final StatisticsService statisticsService;
    private final ScheduledExecutorService sseScheduler;
    private final SimulationStartService simulationStartService;
    private final SimulationRunner simulationRunner;
    private final SimulationResultsCache resultsCache;
    private final SimulationSummariesCache summariesCache;
    private final ReproducibilityBundleService reproducibilityBundleService;
    private final ReproducibilityReplayService reproducibilityReplayService;
    private final RunDiffService runDiffService;
    private final ObjectMapper objectMapper;

    @Value("${settings.runs}")
    private int runs;

    @Value("${settings.batch-size}")
    private int batchSize;

    @Value("${settings.timeout}")
    private long timeout;

    @Value("${simulation.progressStep:1000}")
    private int progressStep;

    public FirecastingController(SimulationQueueService simQueue,
                                 SimulationSseService sseService,
                                 StatisticsService statisticsService,
                                 ScheduledExecutorService sseScheduler,
                                 SimulationStartService simulationStartService,
                                 SimulationRunner simulationRunner,
                                 SimulationResultsCache resultsCache,
                                 SimulationSummariesCache summariesCache,
                                 ReproducibilityBundleService reproducibilityBundleService,
                                 ReproducibilityReplayService reproducibilityReplayService,
                                 RunDiffService runDiffService,
                                 ObjectMapper objectMapper) {
        this.simQueue = simQueue;
        this.sseService = sseService;
        this.statisticsService = statisticsService;
        this.sseScheduler = sseScheduler;
        this.simulationStartService = simulationStartService;
        this.simulationRunner = simulationRunner;
        this.resultsCache = resultsCache;
        this.summariesCache = summariesCache;
        this.reproducibilityBundleService = reproducibilityBundleService;
        this.reproducibilityReplayService = reproducibilityReplayService;
        this.runDiffService = runDiffService;
        this.objectMapper = objectMapper;
    }

    // ------------------------------------------------------------------------------------
    // Runs list + diff
    // ------------------------------------------------------------------------------------

    @GetMapping(value = "/runs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<RunListItemDto>> listRuns(
            @RequestParam(value = "limit", required = false) Integer limit) {
        final int effectiveLimit = (limit == null || limit <= 0) ? 500 : limit;
        var runs = statisticsService.listRecentRuns(effectiveLimit)
                .stream()
                .map(r -> {
                    var d = new RunListItemDto();
                    d.setId(r.getId());
                    d.setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
                    d.setRngSeed(r.getRngSeed());
                    d.setModelAppVersion(r.getModelAppVersion());
                    d.setInputHash(r.getInputHash());
                    return d;
                })
                .toList();
        return ResponseEntity.ok(runs);
    }

    @GetMapping(value = "/runs/{runId}/summaries", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<YearlySummary>> getRunSummaries(@PathVariable String runId) {
        var run = statisticsService.getRun(runId);
        if (run == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(statisticsService.getSummariesForRun(runId));
    }

    @GetMapping(value = "/runs/{runId}/input", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getRunInput(@PathVariable String runId) {
        var run = statisticsService.getRun(runId);
        if (run == null) return ResponseEntity.notFound().build();
        if (run.getInputJson() == null || run.getInputJson().isBlank()) return ResponseEntity.notFound().build();
        try {
            JsonNode inputNode = objectMapper.readTree(run.getInputJson());
            return ResponseEntity.ok(inputNode);
        } catch (Exception e) {
            log.error("Failed to parse persisted inputJson for run {}", runId, e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping(value = "/diff/{runAId}/{runBId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RunDiffResponse> diffRuns(
            @PathVariable String runAId,
            @PathVariable String runBId) {
        RunDiffResponse diff = runDiffService.diff(runAId, runBId);
        return diff == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(diff);
    }

    // ------------------------------------------------------------------------------------
    // Schema endpoints (unchanged)
    // ------------------------------------------------------------------------------------

    @GetMapping("/schema/simulation")
    public List<UISchemaField> getSimulationSchema() {
        return UISchemaGenerator.generateSchema(SimulationRequest.class);
    }

    @GetMapping("/schema/phase")
    public List<UISchemaField> getPhaseSchema() {
        return UISchemaGenerator.generateSchema(PhaseRequest.class);
    }

    // ------------------------------------------------------------------------------------
    // Queue info
    // ------------------------------------------------------------------------------------

    @GetMapping("/queue/{simulationId}")
    public ResponseEntity<SimulationQueueService.TaskInfo> queueInfo(@PathVariable String simulationId) {
        var info = simQueue.info(simulationId);
        return ResponseEntity.ok(info);
    }

    // ------------------------------------------------------------------------------------
    // Start simulation
    // ------------------------------------------------------------------------------------

    @PostMapping(
            value = "/start",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, String>> startSimulation(@Valid @RequestBody SimulationRequest request) {
        ReturnerConfig returnerConfig = null;
        if (request.getSeed() != null) {
            returnerConfig = new ReturnerConfig();
            returnerConfig.setSeed(request.getSeed());
        }

        var spec = new SimulationRunSpec(
                request.getStartDate(),
                request.getPhases(),
                request.getOverallTaxRule(),
                request.getTaxPercentage(),
                "dataDrivenReturn",
                1.02D,
            0.0,
                returnerConfig,
                null
        );
        return simulationStartService.startSimulation("/start", spec, request);
    }

    @PostMapping(
            value = "/start-advanced",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, String>> startAdvancedSimulation(
            @Valid @RequestBody AdvancedSimulationRequest request) {
        var spec = AdvancedSimulationRequestMapper.toRunSpec(request);

        // Dedup is based on hashing the serialized *input DTO*.
        // /start hashes SimulationRequest; /start-advanced hashes AdvancedSimulationRequest.
        // If the advanced request is semantically identical to the legacy defaults, persist/hash
        // a legacy SimulationRequest so it deduplicates with /start.
        if (isLegacyEquivalentAdvancedRequest(request, spec)) {
            SimulationRequest legacy = toLegacySimulationRequest(request, spec);
            return simulationStartService.startSimulation("/start-advanced", spec, legacy);
        }

        return simulationStartService.startSimulation("/start-advanced", spec, request);
    }

    private static boolean isLegacyEquivalentAdvancedRequest(AdvancedSimulationRequest request, SimulationRunSpec spec) {
        if (request == null || spec == null) return false;

        // Legacy endpoint always uses:
        //  - returnType=dataDrivenReturn
        //  - inflationFactor=1.02
        //  - no taxExemptionConfig
        //  - no returnerConfig (unless seed is present, which still becomes returnerConfig)
        String rt = request.getReturnType();
        boolean returnTypeIsLegacy = (rt == null || rt.isBlank()) || "dataDrivenReturn".equals(rt);
        boolean noReturnerOverrides = request.getReturnerConfig() == null && request.getSeed() == null;
        boolean noTaxExemptionOverrides = request.getTaxExemptionConfig() == null;

        // Client may omit inflationFactor (0.0) and mapper defaults to 1.02.
        double inflationFactor = request.getInflationFactor();
        boolean inflationIsLegacy = (inflationFactor <= 0.0) || Math.abs(inflationFactor - 1.02D) < 1e-12;
        boolean specInflationIsLegacy = Math.abs(spec.getInflationFactor() - 1.02D) < 1e-12;

        double yearlyFeePercentage = request.getYearlyFeePercentage();
        boolean feeIsLegacy = !Double.isFinite(yearlyFeePercentage) || Math.abs(yearlyFeePercentage) < 1e-12;
        boolean specFeeIsLegacy = Math.abs(spec.getYearlyFeePercentage()) < 1e-12;

        return returnTypeIsLegacy && noReturnerOverrides && noTaxExemptionOverrides && inflationIsLegacy && specInflationIsLegacy && feeIsLegacy && specFeeIsLegacy;
    }

    private static SimulationRequest toLegacySimulationRequest(AdvancedSimulationRequest request, SimulationRunSpec spec) {
        SimulationRequest legacy = new SimulationRequest();
        legacy.setStartDate(request.getStartDate());
        legacy.setPhases(request.getPhases());

        // Normalize to match typical frontend legacy requests (CAPITAL/NOTIONAL).
        String rule = (spec.getOverallTaxRule() == null) ? null : spec.getOverallTaxRule().trim().toUpperCase();
        legacy.setOverallTaxRule(rule);

        legacy.setTaxPercentage(request.getTaxPercentage());
        // returnPercentage intentionally left at default (0.0f)
        // seed intentionally left null
        return legacy;
    }

    // ------------------------------------------------------------------------------------
    // Progress – JSON (completed summaries from DB)
    // ------------------------------------------------------------------------------------

    @GetMapping(value = "/progress/{simulationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<YearlySummary>> getProgressJson(@PathVariable String simulationId) {
        if (statisticsService.hasCompletedSummaries(simulationId)) {
            var summaries = statisticsService.getSummariesForRun(simulationId);
            return summaries.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(summaries);
        }

        // For stochastic (negative-seed) runs, we intentionally skip DB persistence.
        // Those runs can still be served from the in-memory summaries cache (TTL-based).
        var cached = summariesCache.get(simulationId);
        return (cached == null || cached.isEmpty())
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(cached);
    }

    // ------------------------------------------------------------------------------------
    // Reproducibility bundle export (single JSON file)
    // ------------------------------------------------------------------------------------

    @GetMapping(value = "/{simulationId}/bundle", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> exportRunBundle(
            @PathVariable String simulationId,
            @RequestParam(value = "uiMode", required = false) String uiMode) {

        if (!statisticsService.hasCompletedSummaries(simulationId)) {
            return ResponseEntity.notFound().build();
        }

        var bundle = reproducibilityBundleService.buildBundle(simulationId, uiMode);
        if (bundle == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] json;
        try {
            json = objectMapper.writeValueAsBytes(bundle);
        } catch (Exception e) {
            log.error("Failed to serialize reproducibility bundle for {}", simulationId, e);
            return ResponseEntity.status(500).build();
        }

        String fileName = "firecasting-run-" + simulationId + ".json";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }

    // ------------------------------------------------------------------------------------
    // Reproducibility import/replay
    // ------------------------------------------------------------------------------------

    @PostMapping(
            value = "/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ReplayStartResponse> importRunBundle(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiValidationException("Validation failed", List.of("file: is required"));
        }

        ReproducibilityBundleDto bundle;
        try {
            bundle = objectMapper.readValue(file.getBytes(), ReproducibilityBundleDto.class);
        } catch (JsonProcessingException e) {
            throw new ApiValidationException("Invalid JSON", List.of("file: " + e.getOriginalMessage()), e);
        } catch (Exception e) {
            throw new ApiValidationException("Invalid request", List.of("file: could not be read"), e);
        }

        ReplayStartResponse resp = reproducibilityReplayService.importBundle(bundle);
        return ResponseEntity.accepted().body(resp);
    }

    @PostMapping(
            value = "/import",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ReplayStartResponse> importRunBundleJson(@RequestBody ReproducibilityBundleDto bundle) {
        ReplayStartResponse resp = reproducibilityReplayService.importBundle(bundle);
        return ResponseEntity.accepted().body(resp);
    }

    @GetMapping(value = "/replay/{replayId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReplayStatusResponse> getReplayStatus(@PathVariable String replayId) {
        ReplayStatusResponse status = reproducibilityReplayService.getStatus(replayId);
        return status == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(status);
    }

    // ------------------------------------------------------------------------------------
    // Progress – SSE stream
    // ------------------------------------------------------------------------------------

    @GetMapping(value = "/progress/{simulationId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getProgressSse(@PathVariable String simulationId) {
        // If summaries already exist -> send "completed" immediately and close stream
        if (statisticsService.hasCompletedSummaries(simulationId)) {
            var emitter = new SseEmitter(0L);
            try {
                var summaries = statisticsService.getSummariesForRun(simulationId);
                emitter.send(SseEmitter.event()
                        .name("completed")
                        .data(summaries, MediaType.APPLICATION_JSON));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        // Same behavior for non-persisted (negative-seed) runs, if still cached.
        var cached = summariesCache.get(simulationId);
        if (cached != null && !cached.isEmpty()) {
            var emitter = new SseEmitter(0L);
            try {
                emitter.send(SseEmitter.event()
                        .name("completed")
                        .data(cached, MediaType.APPLICATION_JSON));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        var emitter = new SseEmitter(0L);
        sseService.addEmitter(simulationId, emitter);

        try {
            emitter.send(SseEmitter.event().name("open").data("ok"));
        } catch (Exception ignore) {
        }

        final AtomicReference<ScheduledFuture<?>> handle = new AtomicReference<>();

        Runnable tick = () -> {
            try {
                var info = simQueue.info(simulationId);
                if (info == null || info.getStatus() == null) {
                    if (!"QUEUED".equals(sseService.getLastState(simulationId))) {
                        sseService.enqueueStateQueued(simulationId, "queued");
                        sseService.setLastState(simulationId, "QUEUED");
                    }
                    return;
                }

                String status = info.getStatus().name();
                switch (info.getStatus()) {
                    case QUEUED -> {
                        Integer p = info.getPosition(); // already 0-based
                        Integer prev = sseService.getLastQueuedPos(simulationId);

                        if (p != null) {
                            if (!Objects.equals(prev, p)) {
                                sseService.enqueueStateQueued(simulationId, "position:" + p);
                                sseService.setLastQueuedPos(simulationId, p);
                            }
                        } else {
                            if (!Objects.equals(prev, -1)) {
                                sseService.enqueueStateQueued(simulationId, "queued");
                                sseService.setLastQueuedPos(simulationId, -1);
                            }
                        }

                        if (!"QUEUED".equals(sseService.getLastState(simulationId))) {
                            sseService.setLastState(simulationId, "QUEUED");
                        }
                    }
                    case RUNNING -> {
                        if (!"RUNNING".equals(sseService.getLastState(simulationId))) {
                            sseService.enqueueStateStarted(simulationId, "running");
                            sseService.setLastState(simulationId, "RUNNING");
                        }
                        sseService.setLastQueuedPos(simulationId, null);
                        sseService.enqueueHeartbeat(simulationId);
                    }
                    case DONE, FAILED -> {
                        sseService.setLastState(simulationId, status);
                        sseService.setLastQueuedPos(simulationId, null);
                        var h = handle.get();
                        if (h != null) h.cancel(false);
                    }
                }
            } catch (Exception ignored) {
            }
        };

        handle.set(sseScheduler.scheduleAtFixedRate(tick, 0, 2, TimeUnit.SECONDS));

        emitter.onCompletion(() -> {
            sseService.clearState(simulationId);
            var h = handle.get();
            if (h != null) h.cancel(false);
        });

        emitter.onTimeout(() -> {
            sseService.clearState(simulationId);
            var h = handle.get();
            if (h != null) h.cancel(false);
        });

        return emitter;
    }

    // ------------------------------------------------------------------------------------
    // Export last results as CSV
    // ------------------------------------------------------------------------------------

    @GetMapping("/{simulationId}/export")
    public ResponseEntity<StreamingResponseBody> exportResultsAsCsvForRun(@PathVariable String simulationId) {
        var results = resultsCache.get(simulationId);

        // If this was a dedup hit (run already existed), we may not have the full results in memory.
        // Recompute from persisted input_json on-demand so per-run CSV export still works.
        if (results == null || results.isEmpty()) {
            try {
                var run = statisticsService.getRun(simulationId);
                if (run == null || run.getInputJson() == null || run.getInputJson().isBlank()) {
                    return ResponseEntity.notFound().build();
                }

                final ObjectMapper lenientMapper = objectMapper.copy()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

                JsonNode inputNode = lenientMapper.readTree(run.getInputJson());
                SimulationRunSpec spec;

                if (inputNode != null && inputNode.hasNonNull("returnType")) {
                    // advanced-mode persisted request
                    AdvancedSimulationRequest req = lenientMapper.treeToValue(inputNode, AdvancedSimulationRequest.class);
                    spec = AdvancedSimulationRequestMapper.toRunSpec(req);
                } else {
                    // legacy/normal persisted request
                    SimulationRequest req = lenientMapper.treeToValue(inputNode, SimulationRequest.class);
                    spec = new SimulationRunSpec(
                            req.getStartDate(),
                            req.getPhases(),
                            req.getOverallTaxRule(),
                            req.getTaxPercentage(),
                            "dataDrivenReturn",
                            1.02D
                    );
                }

                // Compute results without persisting (run already exists)
                results = simulationRunner.runSimulationNoPersist(
                        spec,
                        runs,
                        batchSize,
                        msg -> {
                        }
                );

                // Cache for subsequent exports during TTL
                resultsCache.put(simulationId, results);
            } catch (Exception e) {
                log.error("Failed to recompute results for CSV export of {}", simulationId, e);
                return ResponseEntity.status(500).build();
            }
        }

        if (results == null || results.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return exportCsv(results, "simulation-results-" + simulationId);
    }

    @GetMapping("/export")
    public ResponseEntity<StreamingResponseBody> exportResultsAsCsv() {
        var results = resultsCache.getLatest();
        if (results == null || results.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return exportCsv(results, "simulation-results");
    }

    private ResponseEntity<StreamingResponseBody> exportCsv(List<IRunResult> results, String baseFileName) {
        long t0 = System.currentTimeMillis();

        File file;
        try {
            file = ConcurrentCsvExporter.exportCsv(results, baseFileName);
        } catch (IOException e) {
            log.error("Error exporting CSV", e);
            return ResponseEntity.status(500)
                    .body(outputStream -> outputStream.write("Error exporting CSV".getBytes()));
        }

        if (file == null || !file.exists() || file.length() <= 0) {
            log.error("CSV export produced an empty file for {}", baseFileName);
            try {
                if (file != null) Files.deleteIfExists(file.toPath());
            } catch (Exception ignore) {
            }
            return ResponseEntity.status(500)
                    .body(outputStream -> outputStream.write("Error exporting CSV".getBytes()));
        }

        long t1 = System.currentTimeMillis();
        log.info("Handling exports in {} ms", (t1 - t0));

        StreamingResponseBody stream = out -> {
            try {
                Files.copy(file.toPath(), out);
                out.flush();
            } finally {
                try {
                    Files.deleteIfExists(file.toPath());
                } catch (Exception ignore) {
                }
            }
        };

        String downloadFileName = baseFileName + ".csv";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + downloadFileName + "\"")
            .contentLength(file.length())
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(stream);
    }
}
