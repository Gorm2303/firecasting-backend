package dk.gormkrings.statistics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import dk.gormkrings.statistics.mapper.YearlySummaryMapper;
import dk.gormkrings.statistics.mapper.MetricSummaryMapper;
import dk.gormkrings.statistics.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
@Profile("!local") // only active when NOT in local mode
@RequiredArgsConstructor
public class StatisticsService {

    private final SimulationRunRepository runRepo;
    private final YearlySummaryRepository summaryRepo;
    private final MetricSummaryRepository metricSummaryRepo;
    private final @Qualifier("canonicalObjectMapper") ObjectMapper canonicalObjectMapper;
    private final ObjectProvider<BuildProperties> buildPropertiesProvider;

    @PersistenceContext
    private EntityManager em;

    /** lookup an existing runId by input params (dedup) */
    @Transactional(readOnly = true)
    public Optional<String> findExistingRunIdForInput(Object inputParams) {
        String inputJson = toCanonicalJson(inputParams);
        String inputHash = sha256Hex(inputJson);
        return runRepo.findByInputHash(inputHash).map(SimulationRunEntity::getId);
    }

    /**
     * Lookup an existing runId by a signature payload.
     *
     * This is intentionally separate from the persisted input JSON so we can include
     * server-side execution parameters (e.g., number of paths/runs) in the signature
     * without changing what is stored/returned as the user input.
     */
    @Transactional(readOnly = true)
    public Optional<String> findExistingRunIdForSignature(Object signatureParams) {
        String signatureJson = toCanonicalJson(signatureParams);
        String inputHash = sha256Hex(signatureJson);
        return runRepo.findByInputHash(inputHash).map(SimulationRunEntity::getId);
    }

    // NEW: insert-only path for append-only storage
    @Transactional
    public String insertNewRunWithSummaries(String simulationId,
                                           Object inputParams,
                                           Object signatureParams,
                                           Object resolvedAdvanced,
                                           List<YearlySummary> summaries,
                                           List<Double[]> percentileGrids,
                                           List<dk.gormkrings.statistics.MetricSummary> metricSummaries,
                                           Long rngSeed) {
        if (summaries.size() != percentileGrids.size()) {
            throw new IllegalArgumentException("Summaries and grids must have same size.");
        }

        String inputJson = toCanonicalJson(inputParams);
        // Hash signature can differ from persisted inputJson (e.g. include server-side run count)
        Object signature = (signatureParams != null) ? signatureParams : inputParams;
        String signatureJson = toCanonicalJson(signature);
        String inputHash = sha256Hex(signatureJson);
        String resolvedInputJson = resolvedAdvanced != null ? toCanonicalJson(resolvedAdvanced) : null;

        // 1) Persist parent RUN first
        var run = new SimulationRunEntity();
        run.setId(simulationId);                   // we assign the UUID ourselves
        run.setCreatedAt(OffsetDateTime.now());
        run.setInputJson(inputJson);
        run.setResolvedInputJson(resolvedInputJson);
        run.setInputHash(inputHash);

        BuildProperties bp = buildPropertiesProvider.getIfAvailable();
        run.setModelAppVersion(bp != null ? bp.getVersion() : "unknown");
        run.setModelBuildTime(bp != null && bp.getTime() != null ? bp.getTime().toString() : null);
        run.setModelSpringBootVersion(SpringBootVersion.getVersion());
        run.setModelJavaVersion(Runtime.version().toString());
        run.setRngSeed(rngSeed);

        runRepo.save(run);

        // IMPORTANT: flush so the row exists; then use a managed reference for children
        em.flush();
        final SimulationRunEntity runRef = em.getReference(SimulationRunEntity.class, simulationId);

        // 2) Insert children, always setting the managed runRef
        for (int i = 0; i < summaries.size(); i++) {
            var dto  = summaries.get(i);
            var grid = percentileGrids.get(i);
            if (grid == null || grid.length != 101) {
                throw new IllegalArgumentException("Each percentile grid must have length 101.");
            }

            var ent = YearlySummaryMapper.toEntity(dto, runRef, grid); // <-- pass runRef
            // If your mapper does NOT use the given run instance, set it explicitly:
            // ent.setRun(runRef);

            summaryRepo.save(ent);

            // optional batching relief
            // if ((i + 1) % 500 == 0) { em.flush(); em.clear(); }
        }

        if (metricSummaries != null && !metricSummaries.isEmpty()) {
            for (var ms : metricSummaries) {
                metricSummaryRepo.save(MetricSummaryMapper.toEntity(ms, runRef));
            }
        }
        return simulationId;
    }

    /**
     * Backwards compatible overload: uses the input params as the signature as well.
     */
    @Transactional
    public String insertNewRunWithSummaries(String simulationId,
                                           Object inputParams,
                                           Object resolvedAdvanced,
                                           List<YearlySummary> summaries,
                                           List<Double[]> percentileGrids,
                                           Long rngSeed) {
        return insertNewRunWithSummaries(simulationId, inputParams, null, resolvedAdvanced, summaries, percentileGrids, List.of(), rngSeed);
    }

    /**
     * Overload for call sites that provide a separate signature payload.
     * Metric summaries are optional and default to empty.
     */
    @Transactional
    public String insertNewRunWithSummaries(String simulationId,
                                           Object inputParams,
                                           Object signatureParams,
                                           Object resolvedAdvanced,
                                           List<YearlySummary> summaries,
                                           List<Double[]> percentileGrids,
                                           Long rngSeed) {
        return insertNewRunWithSummaries(simulationId, inputParams, signatureParams, resolvedAdvanced, summaries, percentileGrids, List.of(), rngSeed);
    }

    /**
     * Overload keeping call sites readable when providing metric summaries.
     */
    @Transactional
    public String insertNewRunWithSummariesAndMetrics(String simulationId,
                                                      Object inputParams,
                                                      Object signatureParams,
                                                      Object resolvedAdvanced,
                                                      List<YearlySummary> summaries,
                                                      List<Double[]> percentileGrids,
                                                      List<dk.gormkrings.statistics.MetricSummary> metricSummaries,
                                                      Long rngSeed) {
        return insertNewRunWithSummaries(simulationId, inputParams, signatureParams, resolvedAdvanced, summaries, percentileGrids, metricSummaries, rngSeed);
    }

    public boolean hasCompletedSummaries(String runId) {
        return summaryRepo.existsByRunId(runId);
    }

    public boolean hasCompletedMetricSummaries(String runId) {
        return metricSummaryRepo.existsByRunId(runId);
    }

    @Transactional(readOnly = true)
    public List<YearlySummary> getSummariesForRun(String simulationId) {
        return summaryRepo.findByRunIdOrderByPhaseNameAscYearAsc(simulationId)
                .stream()
                .map(YearlySummaryMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<dk.gormkrings.statistics.MetricSummary> getMetricSummariesForRun(String simulationId) {
        return metricSummaryRepo.findByRunIdOrderByScopeAscPhaseNameAscYearAscMetricAsc(simulationId)
            .stream()
            .map(MetricSummaryMapper::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<YearlySummaryEntity> getSummaryEntitiesForRun(String simulationId) {
        return summaryRepo.findByRunIdOrderByPhaseNameAscYearAsc(simulationId);
    }

    @Transactional(readOnly = true)
    public SimulationRunEntity getRun(String simulationId) {
        return runRepo.findById(simulationId).orElse(null);
    }

    @Transactional
    public void updateRunTimings(String runId,
                                 Long computeMs,
                                 Long aggregateMs,
                                 Long gridsMs,
                                 Long persistMs,
                                 Long totalMs) {
        var opt = runRepo.findById(runId);
        if (opt.isEmpty()) return;
        var run = opt.get();

        run.setComputeMs(computeMs != null ? Math.max(0L, computeMs) : null);
        run.setAggregateMs(aggregateMs != null ? Math.max(0L, aggregateMs) : null);
        run.setGridsMs(gridsMs != null ? Math.max(0L, gridsMs) : null);
        run.setPersistMs(persistMs != null ? Math.max(0L, persistMs) : null);
        run.setTotalMs(totalMs != null ? Math.max(0L, totalMs) : null);

        runRepo.save(run);
    }

    public record ModelVersionInfo(
            String modelAppVersion,
            String modelBuildTime,
            String modelSpringBootVersion,
            String modelJavaVersion
    ) {
    }

    /**
     * Best-effort model/runtime version information.
     * Used both for persisted runs and for non-persisted (random seed) runs.
     */
    public ModelVersionInfo getModelVersionInfo() {
        BuildProperties bp = buildPropertiesProvider.getIfAvailable();
        String appVersion = bp != null ? bp.getVersion() : "unknown";
        String buildTime = (bp != null && bp.getTime() != null) ? bp.getTime().toString() : null;
        return new ModelVersionInfo(
                appVersion,
                buildTime,
                SpringBootVersion.getVersion(),
                Runtime.version().toString()
        );
    }

    @Transactional(readOnly = true)
    public List<SimulationRunEntity> listRecentRuns(int limit) {
        int n = Math.max(1, Math.min(limit, 500));
        return runRepo.findAll(org.springframework.data.domain.PageRequest.of(
                        0,
                        n,
                        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")
                ))
                .getContent();
    }

    private String toCanonicalJson(Object input) {
        try {
            ObjectMapper m = (canonicalObjectMapper != null)
                    ? canonicalObjectMapper
                    : JsonMapper.builder()
                    .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                    .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                    .build();
            return m.writeValueAsString(input);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize input for hashing", e);
        }
    }

    private String sha256Hex(String s) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
