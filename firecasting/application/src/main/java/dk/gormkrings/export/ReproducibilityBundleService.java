package dk.gormkrings.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.gormkrings.statistics.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReproducibilityBundleService {

    private final StatisticsService statisticsService;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<BuildProperties> buildPropertiesProvider;

    public ReproducibilityBundleDto buildBundle(String simulationId, String uiMode) {
        var run = statisticsService.getRun(simulationId);
        if (run == null) return null;

        JsonNode inputNode;
        try {
            inputNode = objectMapper.readTree(run.getInputJson());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse persisted inputJson for run " + simulationId, e);
        }

        final String inputKind = inferInputKind(inputNode);

        var meta = new ReproducibilityBundleDto.Meta();
        meta.setSimulationId(simulationId);
        meta.setExportedAt(OffsetDateTime.now().toString());
        // IMPORTANT: uiMode must reflect the persisted inputs used for this run.
        // Never allow a client-supplied parameter to make the bundle claim a different mode.
        meta.setUiMode("advanced".equals(inputKind) ? "advanced" : "normal");
        meta.setInputKind(inputKind);
        meta.setModelVersion(buildModelVersion());

        var inputs = new ReproducibilityBundleDto.Inputs();
        inputs.setKind(inputKind);
        inputs.setRaw(inputNode);
        inputs.setNormal("normal".equals(inputKind) ? inputNode : null);
        inputs.setAdvanced("advanced".equals(inputKind) ? inputNode : null);

        var outputs = new ReproducibilityBundleDto.Outputs();
        outputs.setYearlySummaries(
                statisticsService.getSummaryEntitiesForRun(simulationId)
                        .stream()
                        .map(e -> {
                            var d = new ReproducibilityBundleDto.YearlySummaryWithGrid();
                            d.setPhaseName(e.getPhaseName());
                            d.setYear(e.getYear());
                            d.setAverageCapital(e.getAverageCapital());
                            d.setMedianCapital(e.getMedianCapital());
                            d.setMinCapital(e.getMinCapital());
                            d.setMaxCapital(e.getMaxCapital());
                            d.setStdDevCapital(e.getStdDevCapital());
                            d.setCumulativeGrowthRate(e.getCumulativeGrowthRate());
                            d.setQuantile5(e.getQuantile5());
                            d.setQuantile25(e.getQuantile25());
                            d.setQuantile75(e.getQuantile75());
                            d.setQuantile95(e.getQuantile95());
                            d.setVar(e.getVar());
                            d.setCvar(e.getCvar());
                            d.setNegativeCapitalPercentage(e.getNegativeCapitalPercentage());
                            d.setPercentiles(e.getPercentiles());
                            return d;
                        })
                        .toList()
        );

        var bundle = new ReproducibilityBundleDto();
        bundle.setMeta(meta);
        bundle.setInputs(inputs);
        bundle.setTimeline(buildTimeline(inputNode));
        bundle.setOutputs(outputs);
        return bundle;
    }

    private static ReproducibilityBundleDto.Timeline buildTimeline(JsonNode inputNode) {
        if (inputNode == null || inputNode.isNull()) return null;

        String startDate = extractStartDateIso(inputNode.get("startDate"));
        JsonNode phasesNode = inputNode.get("phases");
        if (startDate == null || phasesNode == null || !phasesNode.isArray() || phasesNode.isEmpty()) return null;

        List<String> phaseTypes = new ArrayList<>();
        List<Integer> phaseDurations = new ArrayList<>();
        Double firstPhaseInitialDeposit = null;

        int idx = 0;
        for (JsonNode p : phasesNode) {
            if (p == null || p.isNull()) continue;
            String phaseType = p.hasNonNull("phaseType") ? p.get("phaseType").asText(null) : null;
            int duration = p.hasNonNull("durationInMonths") ? p.get("durationInMonths").asInt(0) : 0;
            phaseTypes.add(phaseType);
            phaseDurations.add(duration);

            if (idx == 0 && p.hasNonNull("initialDeposit")) {
                firstPhaseInitialDeposit = p.get("initialDeposit").isNumber() ? p.get("initialDeposit").asDouble() : null;
            }
            idx++;
        }

        var t = new ReproducibilityBundleDto.Timeline();
        t.setStartDate(startDate);
        t.setPhaseTypes(phaseTypes);
        t.setPhaseDurationsInMonths(phaseDurations);
        t.setFirstPhaseInitialDeposit(firstPhaseInitialDeposit);
        return t;
    }

    private static String extractStartDateIso(JsonNode startDateNode) {
        if (startDateNode == null || startDateNode.isNull()) return null;

        // Frontend request format: { startDate: { date: "YYYY-MM-DD" } }
        if (startDateNode.isObject() && startDateNode.hasNonNull("date")) {
            String v = startDateNode.get("date").asText(null);
            return (v != null && !v.isBlank()) ? v : null;
        }

        // Backend-persisted LocalDate-like object: {year, month, dayOfMonth, epochDay, ...}
        if (startDateNode.isObject()
                && startDateNode.hasNonNull("year")
                && startDateNode.hasNonNull("month")
                && startDateNode.hasNonNull("dayOfMonth")) {
            int y = startDateNode.get("year").asInt(0);
            int m = startDateNode.get("month").asInt(0);
            int d = startDateNode.get("dayOfMonth").asInt(0);
            if (y > 0 && m >= 1 && m <= 12 && d >= 1 && d <= 31) {
                return String.format("%04d-%02d-%02d", y, m, d);
            }
        }

        if (startDateNode.isObject() && startDateNode.hasNonNull("epochDay")) {
            long epochDay = startDateNode.get("epochDay").asLong(Long.MIN_VALUE);
            if (epochDay != Long.MIN_VALUE) {
                return LocalDate.ofEpochDay(epochDay).toString();
            }
        }

        // As a last resort, accept raw string.
        if (startDateNode.isTextual()) {
            String v = startDateNode.asText(null);
            return (v != null && !v.isBlank()) ? v : null;
        }

        return null;
    }

    private static String inferInputKind(JsonNode input) {
        if (input != null && input.hasNonNull("returnType")) return "advanced";
        return "normal";
    }

    private ReproducibilityBundleDto.ModelVersion buildModelVersion() {
        var mv = new ReproducibilityBundleDto.ModelVersion();

        BuildProperties bp = buildPropertiesProvider.getIfAvailable();
        if (bp != null) {
            mv.setAppVersion(bp.getVersion());
            mv.setBuildTime(bp.getTime() != null ? bp.getTime().toString() : null);
        } else {
            mv.setAppVersion("unknown");
            mv.setBuildTime(null);
        }

        mv.setSpringBootVersion(SpringBootVersion.getVersion());
        mv.setJavaVersion(Runtime.version().toString());
        return mv;
    }
}
