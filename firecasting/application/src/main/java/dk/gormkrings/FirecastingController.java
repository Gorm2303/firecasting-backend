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
import dk.gormkrings.diff.RunDetailsDto;
import dk.gormkrings.diff.RunListItemDto;
import dk.gormkrings.reproducibility.ReplayStartResponse;
import dk.gormkrings.reproducibility.ReplayStatusResponse;
import dk.gormkrings.reproducibility.ReproducibilityReplayService;
import dk.gormkrings.returns.ReturnerConfig;
import dk.gormkrings.tax.TaxExemptionConfig;
import dk.gormkrings.contract.PublicApi;
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
@RequestMapping({"/api/simulation", "/api/simulation/v3"})
@PublicApi
public class FirecastingController {

    // Contract: when the user selects "Default" in the UI we use a fixed deterministic master seed.
    // Negative seeds mean "Random" (generate a new positive seed each run) and are not persisted.
    private static final long DEFAULT_MASTER_SEED = 1L;

    private final SimulationQueueService simQueue;
    private final SimulationSseService sseService;
    private final StatisticsService statisticsService;
    private final ScheduledExecutorService sseScheduler;
    private final SimulationStartService simulationStartService;
    private final SimulationRunner simulationRunner;
    private final SimulationResultsCache resultsCache;
    private final SimulationSummariesCache summariesCache;
    private final ReproducibilityBundleService reproducibilityBundleService;
    private final java.util.Optional<ReproducibilityReplayService> reproducibilityReplayService;
    private final RunDiffService runDiffService;
    private final ObjectMapper objectMapper;
    private final ObjectMapper canonicalObjectMapper;

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
                                 java.util.Optional<ReproducibilityReplayService> reproducibilityReplayService,
                                 RunDiffService runDiffService,
                                 ObjectMapper objectMapper,
                                 @org.springframework.beans.factory.annotation.Qualifier("canonicalObjectMapper") ObjectMapper canonicalObjectMapper) {
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
        this.canonicalObjectMapper = canonicalObjectMapper;
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

    @GetMapping(value = "/runs/{runId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RunDetailsDto> getRunDetails(@PathVariable String runId) {
        var run = statisticsService.getRun(runId);
        if (run == null) return ResponseEntity.notFound().build();

        var d = new RunDetailsDto();
        d.setId(run.getId());
        d.setCreatedAt(run.getCreatedAt() != null ? run.getCreatedAt().toString() : null);
        d.setRngSeed(run.getRngSeed());
        d.setModelAppVersion(run.getModelAppVersion());
        d.setModelBuildTime(run.getModelBuildTime());
        d.setModelSpringBootVersion(run.getModelSpringBootVersion());
        d.setModelJavaVersion(run.getModelJavaVersion());
        d.setInputHash(run.getInputHash());
        d.setComputeMs(run.getComputeMs());
        d.setAggregateMs(run.getAggregateMs());
        d.setGridsMs(run.getGridsMs());
        d.setPersistMs(run.getPersistMs());
        d.setTotalMs(run.getTotalMs());

        return ResponseEntity.ok(d);
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
        
        // Prefer resolved input (with all defaults applied) if available; fall back to raw input
        String inputToReturn = null;
        if (run.getResolvedInputJson() != null && !run.getResolvedInputJson().isBlank()) {
            inputToReturn = run.getResolvedInputJson();
        } else if (run.getInputJson() != null && !run.getInputJson().isBlank()) {
            inputToReturn = run.getInputJson();
        }
        
        if (inputToReturn == null) {
            return ResponseEntity.notFound().build();
        }
        
        try {
            JsonNode inputNode = objectMapper.readTree(inputToReturn);
            return ResponseEntity.ok(inputNode);
        } catch (Exception e) {
            log.error("Failed to parse inputJson for run {}", runId, e);
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping(value = "/runs/lookup", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> findRunByInput(@RequestBody JsonNode input) {
        final int effectivePaths = resolvePathsFromLookupInput(input);
        final int effectiveBatchSize = resolveBatchSizeFromLookupInput(input);
        var match = statisticsService.findExistingRunIdForSignature(
            dk.gormkrings.simulation.SimulationSignature.of(effectivePaths, effectiveBatchSize, input)
        );

        // Backwards compatibility: older runs used a signature without batchSize.
        if (match.isEmpty() && effectiveBatchSize == batchSize) {
            match = statisticsService.findExistingRunIdForSignature(
                    new LegacySimulationSignature(effectivePaths, input)
            );
        }

        if (match.isPresent()) {
            return ResponseEntity.ok(Collections.singletonMap("runId", match.get()));
        }

        // If the caller sent a normal-mode payload but the run was stored as advanced,
        // fall back to the advanced-with-defaults shape for lookup.
        try {
            SimulationRequest normal = objectMapper.treeToValue(input, SimulationRequest.class);
            AdvancedSimulationRequest advanced = convertToAdvancedWithDefaults(normal, effectivePaths, effectiveBatchSize);
            var advMatch = statisticsService.findExistingRunIdForSignature(
                    dk.gormkrings.simulation.SimulationSignature.of(effectivePaths, effectiveBatchSize, advanced)
            );

            if (advMatch.isEmpty() && effectiveBatchSize == batchSize) {
                advMatch = statisticsService.findExistingRunIdForSignature(
                        new LegacySimulationSignature(effectivePaths, advanced)
                );
            }
            if (advMatch.isPresent()) {
                return ResponseEntity.ok(Collections.singletonMap("runId", advMatch.get()));
            }
        } catch (Exception ignored) {
            // Not a normal-mode payload; ignore and fall through to 404.
        }

        // As a last resort, ignore randomness fields when matching templates that don't specify a seed.
        try {
            JsonNode normalizedInput = normalizeForLookup(input);
            JsonNode normalizedNormal = null;
            JsonNode normalizedAdvanced = null;
            try {
                SimulationRequest normal = objectMapper.treeToValue(input, SimulationRequest.class);
                normalizedNormal = normalizeForLookup(canonicalObjectMapper.valueToTree(normal));
                AdvancedSimulationRequest advanced = convertToAdvancedWithDefaults(normal, effectivePaths, effectiveBatchSize);
                normalizedAdvanced = normalizeForLookup(canonicalObjectMapper.valueToTree(advanced));
            } catch (Exception ignored) {
                // Not a normal-mode payload; ignore normal/advanced normalization.
            }

            for (var run : statisticsService.listRecentRuns(500)) {
                String runInputJson = run.getInputJson();
                if (runInputJson == null || runInputJson.isBlank()) continue;
                JsonNode runNode = objectMapper.readTree(runInputJson);
                JsonNode normalizedRun = normalizeForLookup(runNode);
                if (Objects.equals(normalizedInput, normalizedRun)
                        || Objects.equals(normalizedNormal, normalizedRun)
                        || Objects.equals(normalizedAdvanced, normalizedRun)) {
                    return ResponseEntity.ok(Collections.singletonMap("runId", run.getId()));
                }
            }
        } catch (Exception ignored) {
            // Fall through to 404.
        }

        return ResponseEntity.notFound().build();
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
        final Long requestedSeedForDedup = request.getSeed();
        // Convert normal mode request to advanced mode with defaults
        AdvancedSimulationRequest advanced = convertToAdvancedWithDefaults(request, runs, batchSize);
        
        // Map to internal run spec using the same mapper as advanced mode
        var spec = AdvancedSimulationRequestMapper.toRunSpec(advanced);
        
        // Use the resolved advanced request as the canonical input for hashing/persistence.
        // This keeps behavior identical between normal and advanced; only UI visibility differs.
        return simulationStartService.startSimulation("/start", spec, advanced, advanced, null, requestedSeedForDedup);
    }

    /**
     * Convert a normal SimulationRequest to AdvancedSimulationRequest with all defaults applied.
     */
    private static AdvancedSimulationRequest convertToAdvancedWithDefaults(SimulationRequest request, int defaultPaths) {
        AdvancedSimulationRequest advanced = new AdvancedSimulationRequest();
        advanced.setPaths(defaultPaths);
        advanced.setStartDate(request.getStartDate());
        advanced.setPhases(request.getPhases());
        advanced.setOverallTaxRule(request.getOverallTaxRule());
        advanced.setTaxPercentage(request.getTaxPercentage());
        
        // Apply defaults for normal mode
        advanced.setReturnType("dataDrivenReturn");
        advanced.setInflationFactor(1.02D);
        // Legacy behavior uses no yearly fee unless explicitly enabled.
        advanced.setYearlyFeePercentage(0.0);
        
        // Set up seed in returner config
        long seed = normalizeSeed(request.getSeed());
        ReturnerConfig returnerConfig = new ReturnerConfig();
        returnerConfig.setSeed(seed);
        advanced.setReturnerConfig(returnerConfig);
        advanced.setSeed(seed);
        
        // Tax exemption defaults for normal mode
        TaxExemptionConfig.ExemptionCardConfig card = new TaxExemptionConfig.ExemptionCardConfig();
        card.setLimit(51600f);
        card.setYearlyIncrease(1000f);

        TaxExemptionConfig.StockExemptionConfig stock = new TaxExemptionConfig.StockExemptionConfig();
        stock.setTaxRate(27f);      // percentage
        stock.setLimit(67500f);
        stock.setYearlyIncrease(1000f);

        TaxExemptionConfig taxCfg = new TaxExemptionConfig();
        taxCfg.setExemptionCard(card);
        taxCfg.setStockExemption(stock);
        advanced.setTaxExemptionConfig(taxCfg);
        
        return advanced;
    }

    /**
     * Convert a normal SimulationRequest to AdvancedSimulationRequest with all defaults applied.
     */
    private static AdvancedSimulationRequest convertToAdvancedWithDefaults(SimulationRequest request, int defaultPaths, int defaultBatchSize) {
        AdvancedSimulationRequest advanced = convertToAdvancedWithDefaults(request, defaultPaths);
        advanced.setBatchSize(defaultBatchSize);
        return advanced;
    }

    private int resolvePathsFromLookupInput(JsonNode input) {
        if (input != null && input.isObject()) {
            JsonNode p = input.get("paths");
            if (p != null && p.canConvertToInt()) {
                int v = p.asInt();
                if (v > 0) return Math.min(v, 100_000);
            }
        }
        return runs;
    }

    private int resolveBatchSizeFromLookupInput(JsonNode input) {
        if (input != null && input.isObject()) {
            JsonNode b = input.get("batchSize");
            if (b != null && b.canConvertToInt()) {
                int v = b.asInt();
                if (v > 0) return Math.min(v, 100_000);
            }
        }
        return batchSize;
    }

    private static long normalizeSeed(Long seed) {
        if (seed == null) return DEFAULT_MASTER_SEED;
        if (seed < 0) {
            return java.util.concurrent.ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
        }
        return seed;
    }

    private static JsonNode stripRandomnessFields(JsonNode in) {
        if (in == null) return null;
        JsonNode copy = in.deepCopy();
        if (copy.isObject()) {
            ((com.fasterxml.jackson.databind.node.ObjectNode) copy).remove("seed");
            JsonNode rc = copy.get("returnerConfig");
            if (rc != null && rc.isObject()) {
                ((com.fasterxml.jackson.databind.node.ObjectNode) rc).remove("seed");
            }
        }
        return copy;
    }

    private static JsonNode normalizeForLookup(JsonNode in) {
        JsonNode stripped = stripRandomnessFields(in);
        if (stripped == null) return null;
        normalizeStartDate(stripped);
        pruneNulls(stripped);
        return stripped;
    }

    private static void normalizeStartDate(JsonNode node) {
        if (node == null || !node.isObject()) return;
        var obj = (com.fasterxml.jackson.databind.node.ObjectNode) node;
        JsonNode startDate = obj.get("startDate");
        if (startDate != null && startDate.isObject()) {
            var sd = (com.fasterxml.jackson.databind.node.ObjectNode) startDate;
            String dateText = null;
            JsonNode dateNode = sd.get("date");
            if (dateNode != null && dateNode.isTextual()) {
                dateText = dateNode.asText();
            } else {
                JsonNode year = sd.get("year");
                JsonNode month = sd.get("month");
                JsonNode day = sd.get("dayOfMonth");
                if (year != null && month != null && day != null) {
                    try {
                        java.time.LocalDate d = java.time.LocalDate.of(year.asInt(), month.asInt(), day.asInt());
                        dateText = d.toString();
                    } catch (Exception ignored) {
                        // leave as-is
                    }
                }
            }

            if (dateText != null) {
                sd.removeAll();
                sd.put("date", dateText);
            }
        }

        // Remove legacy/system fields that aren't part of the user input shape.
        obj.remove("epochDay");
        obj.remove("returnPercentage");
        obj.remove("totalDurationValid");
    }

    private static void pruneNulls(JsonNode node) {
        if (node == null) return;
        if (node.isObject()) {
            var obj = (com.fasterxml.jackson.databind.node.ObjectNode) node;
            var fieldNames = new java.util.ArrayList<String>();
            obj.fieldNames().forEachRemaining(fieldNames::add);
            for (String name : fieldNames) {
                JsonNode child = obj.get(name);
                if (child == null || child.isNull()) {
                    obj.remove(name);
                } else {
                    pruneNulls(child);
                    // Remove empty objects to normalize missing vs null objects
                    if (child.isObject() && child.size() == 0) {
                        obj.remove(name);
                    }
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                pruneNulls(child);
            }
        }
    }

    @PostMapping(
            value = "/start-advanced",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, String>> startAdvancedSimulation(
            @Valid @RequestBody AdvancedSimulationRequest request) {
        final Long requestedSeedForDedup = (request.getSeed() != null)
            ? request.getSeed()
            : (request.getReturnerConfig() != null ? request.getReturnerConfig().getSeed() : null);
        // Ensure we always have an explicit paths value for persistence/UX consistency.
        if (request.getPaths() == null) request.setPaths(runs);
        if (request.getBatchSize() == null) request.setBatchSize(batchSize);
        var spec = AdvancedSimulationRequestMapper.toRunSpec(request);

        // Canonical behavior: always hash/persist the resolved advanced request.
        return simulationStartService.startSimulation("/start-advanced", spec, request, request, null, requestedSeedForDedup);
    }

    private static boolean isLegacyEquivalentAdvancedRequest(AdvancedSimulationRequest request, SimulationRunSpec spec, int defaultPaths, int defaultBatchSize) {
        if (request == null || spec == null) return false;

        // If paths/runs is explicitly set and differs from default, it is NOT legacy-equivalent.
        if (request.getPaths() != null && !Objects.equals(request.getPaths(), defaultPaths)) {
            return false;
        }

        // If batchSize is explicitly set and differs from default, it is NOT legacy-equivalent.
        if (request.getBatchSize() != null && request.getBatchSize() > 0 && request.getBatchSize() != defaultBatchSize) {
            return false;
        }

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

    /**
     * Legacy signature payload used before batchSize participated in the signature.
     */
    private record LegacySimulationSignature(int paths, Object input) {
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
        if (reproducibilityReplayService.isEmpty()) {
            return ResponseEntity.status(501).body(null);
        }
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

        ReplayStartResponse resp = reproducibilityReplayService.get().importBundle(bundle);
        return ResponseEntity.accepted().body(resp);
    }

    @PostMapping(
            value = "/import",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ReplayStartResponse> importRunBundleJson(@RequestBody ReproducibilityBundleDto bundle) {
        if (reproducibilityReplayService.isEmpty()) {
            return ResponseEntity.status(501).body(null);
        }
        ReplayStartResponse resp = reproducibilityReplayService.get().importBundle(bundle);
        return ResponseEntity.accepted().body(resp);
    }

    @GetMapping(value = "/replay/{replayId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReplayStatusResponse> getReplayStatus(@PathVariable String replayId) {
        if (reproducibilityReplayService.isEmpty()) {
            return ResponseEntity.status(501).build();
        }
        ReplayStatusResponse status = reproducibilityReplayService.get().getStatus(replayId);
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
                var meta = new HashMap<String, Object>();
                meta.put("dedupHit", true);
                meta.put("source", "db");
                meta.put("persisted", true);
                meta.put("queueMs", 0);

                try {
                    var run = statisticsService.getRun(simulationId);
                    if (run != null) {
                        if (run.getComputeMs() != null) meta.put("computeMs", run.getComputeMs());
                        if (run.getAggregateMs() != null) meta.put("aggregateMs", run.getAggregateMs());
                        if (run.getGridsMs() != null) meta.put("gridsMs", run.getGridsMs());
                        if (run.getPersistMs() != null) meta.put("persistMs", run.getPersistMs());
                        if (run.getTotalMs() != null) meta.put("totalMs", run.getTotalMs());
                    }
                } catch (Exception ignore) {
                    // best-effort
                }

                emitter.send(SseEmitter.event()
                        .name("meta")
                        .data(meta, MediaType.APPLICATION_JSON));
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
                        .name("meta")
                        .data(Map.of(
                                "dedupHit", false,
                                "source", "cache"
                        ), MediaType.APPLICATION_JSON));
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
