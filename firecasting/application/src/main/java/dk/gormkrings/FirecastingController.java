package dk.gormkrings;

import dk.gormkrings.action.*;
import dk.gormkrings.dto.PhaseRequest;
import dk.gormkrings.dto.SimulationRequest;
import dk.gormkrings.factory.IDateFactory;
import dk.gormkrings.factory.IPhaseFactory;
import dk.gormkrings.factory.ISimulationFactory;
import dk.gormkrings.factory.ISpecificationFactory;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.result.IRunResult;
import dk.gormkrings.simulation.util.ConcurrentCsvExporter;
import dk.gormkrings.statistics.SimulationAggregationService;
import dk.gormkrings.statistics.YearlySummary;
import dk.gormkrings.tax.ITaxExemptionFactory;
import dk.gormkrings.tax.ITaxExemption;
import dk.gormkrings.tax.ITaxRule;
import dk.gormkrings.tax.ITaxRuleFactory;
import dk.gormkrings.ui.fields.UISchemaField;
import dk.gormkrings.ui.generator.UISchemaGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequestMapping("/api/simulation")
public class FirecastingController {

    private final ISimulationFactory simulationFactory;
    private final IDateFactory dateFactory;
    private final IPhaseFactory phaseFactory;
    private final ISpecificationFactory specificationFactory;
    private final SimulationAggregationService aggregationService;
    private final ITaxRuleFactory taxRuleFactory;
    private final ITaxExemptionFactory taxExemptionFactory;
    private final IActionFactory actionFactory;

    @Value("${settings.runs}")
    private int runs;
    @Value("${settings.batch-size}")
    private int batchSize;
    @Value("${settings.timeout}")
    private long timeout = 60000;

    // Field to store the simulation results for later export.
    private List<IRunResult> lastResults;

    // Map to store SSE emitters by simulation ID.
    private final ConcurrentHashMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public FirecastingController(ISimulationFactory simulationFactory,
                                 IDateFactory dateFactory,
                                 IPhaseFactory phaseFactory,
                                 ISpecificationFactory specificationFactory,
                                 SimulationAggregationService aggregationService,
                                 ITaxRuleFactory taxRuleFactory,
                                 ITaxExemptionFactory taxExemptionFactory,
                                 IActionFactory actionFactory) {
        this.simulationFactory = simulationFactory;
        this.dateFactory = dateFactory;
        this.phaseFactory = phaseFactory;
        this.specificationFactory = specificationFactory;
        this.aggregationService = aggregationService;
        this.taxRuleFactory = taxRuleFactory;
        this.taxExemptionFactory = taxExemptionFactory;
        this.actionFactory = actionFactory;
    }

    @GetMapping("/schema/simulation")
    public List<UISchemaField> getSimulationSchema() {
        return UISchemaGenerator.generateSchema(SimulationRequest.class);
    }

    @GetMapping("/schema/phase")
    public List<UISchemaField> getPhaseSchema() {
        return UISchemaGenerator.generateSchema(PhaseRequest.class);
    }
    
    @PostMapping("/start")
    public ResponseEntity<String> startSimulation(@RequestBody SimulationRequest request) {
        // Generate a unique simulation ID.
        String simulationId = UUID.randomUUID().toString();
        log.info("Starting simulation with id: {} - {}", simulationId, request.getOverallTaxRule());

        float taxPercentage = request.getTaxPercentage();
        ITaxRule overAllTaxRule = taxRuleFactory.create(request.getOverallTaxRule(), taxPercentage);
        // Build the simulation specification.
        var specification = specificationFactory.create(
                request.getEpochDay(), overAllTaxRule, 2F);

        var currentDate = dateFactory.dateOf(
                request.getStartDate().getYear(),
                request.getStartDate().getMonth(),
                request.getStartDate().getDayOfMonth());

        List<IPhase> phases = new LinkedList<>();

        for (PhaseRequest pr : request.getPhases()) {
            List<ITaxExemption> taxExemptions = new LinkedList<>();

            for (String taxExemption : pr.getTaxRules()) {
                taxExemptions.add(taxExemptionFactory.create(taxExemption));
            }
            long days = currentDate.daysUntil(currentDate.plusMonths(pr.getDurationInMonths()));
            String phaseType = pr.getPhaseType().toLowerCase();

            IAction action = switch (phaseType) {
                case "deposit" -> actionFactory.createDepositAction(
                        pr.getInitialDeposit(), pr.getMonthlyDeposit(), pr.getYearlyIncreaseInPercentage()
                );
                case "withdraw" -> actionFactory.createWithdrawAction(
                        pr.getWithdrawAmount(), pr.getWithdrawRate(), pr.getLowerVariationPercentage(), pr.getUpperVariationPercentage()
                );
                case "passive" -> actionFactory.createPassiveAction();
                default -> throw new IllegalArgumentException("Unknown phase type: " + phaseType);
            };

            phases.add(phaseFactory.create(phaseType, specification, currentDate, taxExemptions, days, action));
            currentDate = currentDate.plusMonths(pr.getDurationInMonths());
        }

        // Run the simulation asynchronously.
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                // runWithProgress should accept a lambda progress callback.
                // Each time a block (e.g. 10,000 runs) is completed, the callback is invoked with a progress message.
                List<IRunResult> simulationResults = simulationFactory.createSimulation().runWithProgress(
                        runs,
                        batchSize,
                        phases,
                        progressMessage -> emitterSend(progressMessage, simulationId)
                );

                log.debug("SimulationResults count = {}", simulationResults.size());
                if (!simulationResults.isEmpty()) {
                    log.debug("  first result: {}", simulationResults.get(0));
                }

                // Save results for potential export.
                lastResults = simulationResults;

                // Once simulation finishes, aggregate the results.
                List<YearlySummary> summaries = aggregationService.aggregateResults(
                        simulationResults,
                        progressMessage -> emitterSend(progressMessage, simulationId));

                log.debug("Summaries count = {}", summaries.size());
                log.debug("Summaries = {}", summaries);

                // Send the final aggregated statistics to the SSE emitter.
                SseEmitter emitter = emitters.get(simulationId);
                if (emitter != null) {
                    emitter.send(summaries, MediaType.APPLICATION_JSON);
                    emitter.complete();
                }
            } catch (Exception e) {
                log.error("Error during simulation (id: {})", simulationId, e);
                SseEmitter emitter = emitters.get(simulationId);
                if (emitter != null) {
                    emitter.completeWithError(e);
                }
            }
        });

        // Return the simulation ID so the frontend can subscribe to progress.
        return ResponseEntity.ok(simulationId);
    }

    private void emitterSend(String progressMessage, String simulationId) {
        // If an SSE emitter is registered for this simulation, send the update.
        SseEmitter emitter = emitters.get(simulationId);
        if (emitter != null) {
            try {
                emitter.send(progressMessage, MediaType.TEXT_PLAIN);
            } catch (Exception e) {
                log.error("Error sending progress update for simulationId {}: {}", simulationId, e.getMessage());
            }
        }
    }

    @GetMapping(
            path     = "/progress/{simulationId}",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter getProgress(@PathVariable String simulationId) {
        SseEmitter emitter = new SseEmitter(0L);

        // Put the emitter in the map, so that simulation progress updates can find it.
        emitters.put(simulationId, emitter);

        // Remove the emitter from the map if it completes or times out.
        emitter.onCompletion(() -> emitters.remove(simulationId));
        emitter.onTimeout(() -> emitters.remove(simulationId));

        return emitter;
    }

    @GetMapping("/export")
    public ResponseEntity<StreamingResponseBody> exportResultsAsCsv() {
        if (lastResults == null || lastResults.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        long simTime = System.currentTimeMillis();
        File file;
        try {
            file = ConcurrentCsvExporter.exportCsv(lastResults, "simulation-results");
        } catch (IOException e) {
            log.error("Error exporting CSV", e);
            return ResponseEntity.status(500).body(outputStream -> {
                outputStream.write("Error exporting CSV".getBytes());
            });
        }
        long exportTime = System.currentTimeMillis();
        log.info("Handling exports in {} ms", exportTime - simTime);

        StreamingResponseBody stream = out -> {
            Files.copy(file.toPath(), out);
            out.flush();
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(stream);
    }
}
