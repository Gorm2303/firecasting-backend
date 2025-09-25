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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
@RequiredArgsConstructor
public class StatisticsService {

    private final SimulationRunRepository runRepo;
    private final YearlySummaryRepository summaryRepo;
    private final @Qualifier("canonicalObjectMapper") ObjectMapper canonicalObjectMapper;

    @PersistenceContext
    private EntityManager em;

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

    /** lookup an existing runId by input params (dedup) */
    @Transactional(readOnly = true)
    public Optional<String> findExistingRunIdForInput(Object inputParams) {
        String inputJson = toCanonicalJson(inputParams);
        String inputHash = sha256Hex(inputJson);
        return runRepo.findByInputHash(inputHash).map(SimulationRunEntity::getId);
    }

    /**
     * Orchestrator: NO transaction here.
     * Does: upsert header (tiny TX), bulk-delete old summaries (tiny TX),
     *       insert new summaries in small chunks (many tiny TXs).
     */
    public String upsertRunWithSummaries(String simulationId,
                                         Object inputParams,
                                         List<YearlySummary> summaries,
                                         List<Double[]> percentileGrids) {
        if (summaries.size() != percentileGrids.size()) {
            throw new IllegalArgumentException("Summaries and grids must have same size.");
        }

        // Canonical input (consistent with dedup)
        String inputJson = toCanonicalJson(inputParams);
        String inputHash = sha256Hex(inputJson);

        // 1) upsert header/meta in its own short TX
        upsertRunHeader(simulationId, inputJson, inputHash);

        // 2) remove existing children with ONE SQL (don’t load the collection)
        deleteSummariesByRun(simulationId);

        // 3) insert new children in chunks to keep each TX short and use JDBC batching
        final int CHUNK = 500; // tune: 200–1000 depending on DB
        for (int i = 0; i < summaries.size(); i += CHUNK) {
            int j = Math.min(i + CHUNK, summaries.size());
            insertSummariesChunk(simulationId, summaries, percentileGrids, i, j);
        }

        return simulationId;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void upsertRunHeader(String simulationId, String inputJson, String inputHash) {
        // get a reference to avoid loading children
        SimulationRunEntity run = runRepo.findById(simulationId).orElseGet(() -> {
            SimulationRunEntity r = new SimulationRunEntity();
            r.setId(simulationId);
            r.setCreatedAt(OffsetDateTime.now());
            return r;
        });
        run.setInputJson(inputJson);
        run.setInputHash(inputHash);
        runRepo.save(run);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void deleteSummariesByRun(String simulationId) {
        summaryRepo.deleteByRunId(simulationId); // bulk delete JPQL/SQL
        // ensure delete is flushed now
        em.flush();
        em.clear();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void insertSummariesChunk(String simulationId,
                                        List<YearlySummary> summaries,
                                        List<Double[]> grids,
                                        int from, int to) {

        // Avoid loading the parent: use a reference
        SimulationRunEntity runRef = em.getReference(SimulationRunEntity.class, simulationId);

        for (int k = from; k < to; k++) {
            YearlySummary dto = summaries.get(k);
            Double[] grid = grids.get(k);
            if (grid == null || grid.length != 101) {
                throw new IllegalArgumentException("Each percentile grid must have length 101.");
            }
            var ent = YearlySummaryMapper.toEntity(dto, runRef, grid);
            summaryRepo.save(ent);
        }

        // Trigger batched INSERTs now, then detach to keep persistence context small
        em.flush();
        em.clear();
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
}
