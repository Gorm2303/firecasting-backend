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
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public String upsertRunWithSummaries(String simulationId,
                                         Object inputParams,
                                         List<YearlySummary> summaries,
                                         List<double[]> percentileGrids) {
        if (summaries.size() != percentileGrids.size()) {
            throw new IllegalArgumentException("Summaries and grids must have same size.");
        }

        // 🔁 use the same canonicalization for storage
        String inputJson = toCanonicalJson(inputParams);
        String inputHash = sha256Hex(inputJson);

        SimulationRunEntity run = runRepo.findById(simulationId).orElseGet(() -> {
            SimulationRunEntity r = new SimulationRunEntity();
            r.setId(simulationId);
            r.setCreatedAt(OffsetDateTime.now());
            return r;
        });
        run.setInputJson(inputJson);
        run.setInputHash(inputHash);

        run.getSummaries().clear();
        for (int i = 0; i < summaries.size(); i++) {
            YearlySummary dto = summaries.get(i);
            double[] grid = percentileGrids.get(i);
            if (grid == null || grid.length != 1001) {
                throw new IllegalArgumentException("Each percentile grid must have length 1001.");
            }
            run.getSummaries().add(YearlySummaryMapper.toEntity(dto, run, grid));
        }

        return runRepo.save(run).getId();
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
