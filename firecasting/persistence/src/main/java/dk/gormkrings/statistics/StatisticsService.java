package dk.gormkrings.statistics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.gormkrings.statistics.mapper.YearlySummaryMapper;
import dk.gormkrings.statistics.persistence.*;
import lombok.RequiredArgsConstructor;
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
    private final ObjectMapper objectMapper;

    private String sha256Hex(String s) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // StatisticsService.java
    @Transactional(readOnly = true)
    public Optional<String> findExistingRunIdForInput(Object inputParams) {
        try {
            String inputJson = objectMapper.writeValueAsString(inputParams); // same serializer as upsert
            String inputHash = sha256Hex(inputJson);
            return runRepo.findByInputHash(inputHash).map(SimulationRunEntity::getId);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    @Transactional
    public String upsertRunWithSummaries(
            String simulationId,
            Object inputParams,
            List<YearlySummary> summaries,      // your DTO with scalar stats
            List<double[]> percentileGrids      // parallel list: each 1001-length array
    ) {
        if (summaries.size() != percentileGrids.size()) {
            throw new IllegalArgumentException("Summaries and grids must have same size.");
        }

        String inputJson;
        try { inputJson = objectMapper.writeValueAsString(inputParams); }
        catch (JsonProcessingException e) { throw new IllegalStateException(e); }

        String inputHash = sha256Hex(inputJson);

        // Create or update (replace summaries)
        SimulationRunEntity run = runRepo.findById(simulationId).orElseGet(() -> {
            SimulationRunEntity r = new SimulationRunEntity();
            r.setId(simulationId);               // <— you provide it
            r.setCreatedAt(OffsetDateTime.now());
            return r;
        });
        run.setInputJson(inputJson);
        run.setInputHash(inputHash);

        // Replace all summaries for this run
        run.getSummaries().clear();
        for (int i = 0; i < summaries.size(); i++) {
            YearlySummary dto = summaries.get(i);
            double[] grid = percentileGrids.get(i);
            if (grid == null || grid.length != 1001) {
                throw new IllegalArgumentException("Each percentile grid must have length 1001.");
            }

            YearlySummaryEntity e = YearlySummaryMapper.toEntity(dto, run, grid);

            run.getSummaries().add(e);
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
