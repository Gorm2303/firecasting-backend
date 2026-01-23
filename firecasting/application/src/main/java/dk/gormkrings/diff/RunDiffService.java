package dk.gormkrings.diff;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.gormkrings.statistics.StatisticsService;
import dk.gormkrings.statistics.persistence.SimulationRunEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RunDiffService {

    private static final double EPS = 1e-9;

    private final StatisticsService statisticsService;
    private final ObjectMapper objectMapper;

    public RunDiffResponse diff(String runAId, String runBId) {
        SimulationRunEntity a = statisticsService.getRun(runAId);
        SimulationRunEntity b = statisticsService.getRun(runBId);
        if (a == null || b == null) return null;

        var aSummaries = statisticsService.getSummaryEntitiesForRun(runAId);
        var bSummaries = statisticsService.getSummaryEntitiesForRun(runBId);

        RunDiffResponse resp = new RunDiffResponse();
        resp.setA(toInfo(a));
        resp.setB(toInfo(b));

        resp.setOutput(RunDiffComparator.compare(aSummaries, bSummaries, EPS));
        resp.setAttribution(buildAttribution(a, b));
        return resp;
    }

    private static RunDiffResponse.RunInfo toInfo(SimulationRunEntity e) {
        var i = new RunDiffResponse.RunInfo();
        i.setId(e.getId());
        i.setCreatedAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
        i.setInputHash(e.getInputHash());
        i.setRngSeed(e.getRngSeed());
        i.setModelAppVersion(e.getModelAppVersion());
        return i;
    }

    private RunDiffResponse.Attribution buildAttribution(SimulationRunEntity a, SimulationRunEntity b) {
        var att = new RunDiffResponse.Attribution();

        boolean modelVersionChanged = !Objects.equals(nullToUnknown(a.getModelAppVersion()), nullToUnknown(b.getModelAppVersion()));
        boolean hashChanged = !Objects.equals(a.getInputHash(), b.getInputHash());

        boolean randomnessChanged = false;
        boolean inputsChanged = false;

        if (hashChanged) {
            // Try to isolate RNG-only changes (seed / returnerConfig.seed) from general input changes.
            try {
                JsonNode aj = objectMapper.readTree(a.getInputJson());
                JsonNode bj = objectMapper.readTree(b.getInputJson());

                JsonNode an = stripRandomnessFields(aj);
                JsonNode bn = stripRandomnessFields(bj);

                if (Objects.equals(an, bn)) {
                    randomnessChanged = true;
                } else {
                    inputsChanged = true;
                }
            } catch (Exception e) {
                // If we can't parse, fall back to conservative classification.
                inputsChanged = true;
            }
        } else {
            // Same input hash. If seed differs (or one missing), treat it as randomness change.
            randomnessChanged = !Objects.equals(a.getRngSeed(), b.getRngSeed());
        }

        att.setInputsChanged(inputsChanged);
        att.setRandomnessChanged(randomnessChanged);
        att.setModelVersionChanged(modelVersionChanged);

        att.setSummary(buildSummary(inputsChanged, randomnessChanged, modelVersionChanged));
        return att;
    }

    private static String nullToUnknown(String v) {
        if (v == null || v.isBlank()) return "unknown";
        return v;
    }

    private static String buildSummary(boolean inputsChanged, boolean randomnessChanged, boolean modelVersionChanged) {
        var parts = new ArrayList<String>();
        if (inputsChanged) parts.add("inputs");
        if (randomnessChanged) parts.add("randomness");
        if (modelVersionChanged) parts.add("model version");

        if (parts.isEmpty()) {
            return "No input/model/randomness differences detected; output differences (if any) are likely due to nondeterminism or floating point/runtime differences.";
        }
        if (parts.size() == 1) {
            return "Differences most likely attributable to: " + parts.getFirst() + ".";
        }
        return "Differences may be attributable to: " + String.join(", ", parts) + ".";
    }

    private static JsonNode stripRandomnessFields(JsonNode in) {
        if (in == null) return null;
        // Deep-copy by round-tripping through treeToValue/writeValueAsString is expensive;
        // but inputs are small and this endpoint is interactive.
        // We clone by converting to mutable ObjectNode when possible.

        // We must not mutate the original JsonNode (it may be cached/used elsewhere).
        JsonNode copy = in.deepCopy();

        if (copy.isObject()) {
            ((com.fasterxml.jackson.databind.node.ObjectNode) copy).remove("seed");
            // returnerConfig.seed
            JsonNode rc = copy.get("returnerConfig");
            if (rc != null && rc.isObject()) {
                ((com.fasterxml.jackson.databind.node.ObjectNode) rc).remove("seed");
            }
        }

        return copy;
    }
}
