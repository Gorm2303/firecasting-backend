package dk.gormkrings.reproducibility;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.gormkrings.dto.AdvancedSimulationRequest;
import dk.gormkrings.dto.SimulationRequest;
import dk.gormkrings.export.ReproducibilityBundleDto;
import dk.gormkrings.reproducibility.persistence.ReproducibilityReplayEntity;
import dk.gormkrings.reproducibility.persistence.ReproducibilityReplayRepository;
import dk.gormkrings.simulation.AdvancedSimulationRequestMapper;
import dk.gormkrings.simulation.SimulationRunSpec;
import dk.gormkrings.simulation.SimulationStartService;
import dk.gormkrings.statistics.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReproducibilityReplayService {

    private static final double EPS = 1e-9;

    private final ObjectMapper objectMapper;
    private final StatisticsService statisticsService;
    private final SimulationStartService simulationStartService;
    private final ReproducibilityReplayRepository replayRepo;
    private final Optional<BuildProperties> buildProperties;

    public ReplayStartResponse importBundle(ReproducibilityBundleDto bundle) {
        if (bundle == null) {
            throw new IllegalArgumentException("bundle must not be null");
        }

        String currentAppVersion = buildProperties.map(BuildProperties::getVersion).orElse("unknown");
        String sourceAppVersion = (bundle.getMeta() != null && bundle.getMeta().getModelVersion() != null)
                ? bundle.getMeta().getModelVersion().getAppVersion()
                : null;

        String bundleJson;
        try {
            bundleJson = objectMapper.writeValueAsString(bundle);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize bundle", e);
        }

        var replay = new ReproducibilityReplayEntity();
        replay.setStatus(ReproducibilityReplayEntity.Status.QUEUED.name());
        replay.setBundleJson(bundleJson);
        replay.setSourceAppVersion(sourceAppVersion);
        replay.setCurrentAppVersion(currentAppVersion);
        replay = replayRepo.save(replay);
        final String replayId = replay.getId();

        // Build request + spec
        Object input;
        SimulationRunSpec spec;

        String kind = (bundle.getInputs() != null) ? bundle.getInputs().getKind() : null;
        if ("advanced".equalsIgnoreCase(kind)) {
            AdvancedSimulationRequest req;
            try {
                req = objectMapper.treeToValue(bundle.getInputs().getRaw(), AdvancedSimulationRequest.class);
            } catch (Exception e) {
                throw new IllegalArgumentException("Bundle advanced input could not be parsed", e);
            }
            spec = AdvancedSimulationRequestMapper.toRunSpec(req);
            input = req;
        } else {
            SimulationRequest req;
            try {
                req = objectMapper.treeToValue(bundle.getInputs().getRaw(), SimulationRequest.class);
            } catch (Exception e) {
                throw new IllegalArgumentException("Bundle normal input could not be parsed", e);
            }
            spec = new SimulationRunSpec(
                    req.getStartDate(),
                    req.getPhases(),
                    req.getOverallTaxRule(),
                    req.getTaxPercentage(),
                    "dataDrivenReturn",
                    1.02D
            );
            input = req;
        }

        // Start replay run; when done compare actual DB summaries to expected bundle outputs
        ResponseEntity<Map<String, String>> started = simulationStartService.startSimulation(
                "/import",
                spec,
                input,
            simId -> finalizeReplay(replayId, simId)
        );

        String simulationId = started.getBody() != null ? started.getBody().get("id") : null;
        if (simulationId == null && started.getBody() != null) {
            simulationId = started.getBody().get("error");
        }

        // If dedup hit, we get 200 {id}; it still used the hook (no job). Finalize immediately.
        if (started.getStatusCode().is2xxSuccessful() && started.getStatusCode().value() == 200) {
            finalizeReplay(replayId, started.getBody().get("id"));
        }

        var resp = new ReplayStartResponse();
        resp.setReplayId(replayId);
        resp.setSimulationId(started.getBody() != null ? started.getBody().get("id") : null);
        resp.setStatus(started.getStatusCode().toString());

        String note = "";
        if (sourceAppVersion != null && !sourceAppVersion.equals(currentAppVersion)) {
            note = "Model version mismatch: source=" + sourceAppVersion + ", current=" + currentAppVersion;
        }
        resp.setNote(note);
        return resp;
    }

    public ReplayStatusResponse getStatus(String replayId) {
        var replay = replayRepo.findById(replayId).orElse(null);
        if (replay == null) return null;

        var resp = new ReplayStatusResponse();
        resp.setReplayId(replay.getId());
        resp.setStatus(replay.getStatus());
        resp.setSimulationId(replay.getReplayRunId());

        if (replay.getReportJson() != null) {
            try {
                var node = objectMapper.readTree(replay.getReportJson());
                resp.setExactMatch(node.path("exactMatch").asBoolean(false));
                resp.setWithinTolerance(node.path("withinTolerance").asBoolean(false));
                resp.setMismatches(node.path("mismatches").asInt(0));
                resp.setMaxAbsDiff(node.path("maxAbsDiff").asDouble(0.0));
                resp.setNote(node.path("note").asText(null));
            } catch (Exception ignore) {
                resp.setNote("Failed to parse report_json");
            }
        }

        return resp;
    }

    private void finalizeReplay(String replayId, String simulationId) {
        var replay = replayRepo.findById(replayId).orElse(null);
        if (replay == null) return;

        replay.setReplayRunId(simulationId);
        replay.setStatus(ReproducibilityReplayEntity.Status.RUNNING.name());
        replayRepo.save(replay);

        try {
            var run = statisticsService.getRun(simulationId);
            var actual = statisticsService.getSummaryEntitiesForRun(simulationId);

            var bundle = objectMapper.readValue(replay.getBundleJson(), ReproducibilityBundleDto.class);
            var expected = (bundle.getOutputs() != null) ? bundle.getOutputs().getYearlySummaries() : null;

            var res = ReproducibilityReplayComparator.compare(actual, expected, EPS);

            boolean versionOk = true;
            if (bundle.getMeta() != null && bundle.getMeta().getModelVersion() != null) {
                String source = bundle.getMeta().getModelVersion().getAppVersion();
                String current = replay.getCurrentAppVersion();
                if (source != null && current != null && !source.equals(current)) {
                    versionOk = false;
                }
            }

            String note = versionOk ? null : ("Model version differs; exact reproduction is not guaranteed.");
            String reportJson = objectMapper.writeValueAsString(Map.of(
                    "exactMatch", res.exactMatch(),
                    "withinTolerance", res.withinTolerance(),
                    "mismatches", res.mismatches(),
                    "maxAbsDiff", res.maxAbsDiff(),
                    "note", note
            ));
            replay.setReportJson(reportJson);
            replay.setStatus(ReproducibilityReplayEntity.Status.DONE.name());
            replayRepo.save(replay);
        } catch (Exception e) {
            try {
                replay.setStatus(ReproducibilityReplayEntity.Status.FAILED.name());
                replay.setReportJson(objectMapper.writeValueAsString(Map.of(
                        "exactMatch", false,
                        "withinTolerance", false,
                        "mismatches", 1,
                        "maxAbsDiff", 0.0,
                        "note", "Replay verification failed: " + e.getMessage()
                )));
                replayRepo.save(replay);
            } catch (Exception ignore) {
            }
        }
    }
}
