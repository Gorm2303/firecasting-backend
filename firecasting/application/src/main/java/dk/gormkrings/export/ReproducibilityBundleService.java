package dk.gormkrings.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.gormkrings.statistics.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

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
        meta.setUiMode(normalizeUiMode(uiMode));
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
        bundle.setOutputs(outputs);
        return bundle;
    }

    private static String inferInputKind(JsonNode input) {
        if (input != null && input.hasNonNull("returnType")) return "advanced";
        return "normal";
    }

    private static String normalizeUiMode(String uiMode) {
        String v = String.valueOf(uiMode == null ? "" : uiMode).trim().toLowerCase();
        return v.equals("advanced") ? "advanced" : "normal";
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
