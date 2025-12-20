package dk.gormkrings.statistics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import dk.gormkrings.statistics.mapper.YearlySummaryMapper;
import dk.gormkrings.statistics.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
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
    private final @Qualifier("canonicalObjectMapper") ObjectMapper canonicalObjectMapper;

    @PersistenceContext
    private EntityManager em;

    /** lookup an existing runId by input params (dedup) */
    @Transactional(readOnly = true)
    public Optional<String> findExistingRunIdForInput(Object inputParams) {
        String inputJson = toCanonicalJson(inputParams);
        String inputHash = sha256Hex(inputJson);
        return runRepo.findByInputHash(inputHash).map(SimulationRunEntity::getId);
    }

    // NEW: insert-only path for append-only storage
    @Transactional
    public String insertNewRunWithSummaries(String simulationId,
                                            Object inputParams,
                                            List<YearlySummary> summaries,
                                            List<Double[]> percentileGrids) {
        if (summaries.size() != percentileGrids.size()) {
            throw new IllegalArgumentException("Summaries and grids must have same size.");
        }

        String inputJson = toCanonicalJson(inputParams);
        String inputHash = sha256Hex(inputJson);

        // 1) Persist parent RUN first
        var run = new SimulationRunEntity();
        run.setId(simulationId);                   // we assign the UUID ourselves
        run.setCreatedAt(OffsetDateTime.now());
        run.setInputJson(inputJson);
        run.setInputHash(inputHash);
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
        return simulationId;
    }

    public boolean hasCompletedSummaries(String runId) {
        return summaryRepo.existsByRunId(runId);
    }

    @Transactional(readOnly = true)
    public List<YearlySummary> getSummariesForRun(String simulationId) {
        return summaryRepo.findByRunIdOrderByPhaseNameAscYearAsc(simulationId)
                .stream()
                .map(YearlySummaryMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SimulationRunEntity getRun(String simulationId) {
        return runRepo.findById(simulationId).orElse(null);
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
