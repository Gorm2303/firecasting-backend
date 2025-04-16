package dk.gormkrings;

import dk.gormkrings.action.Deposit;
import dk.gormkrings.action.IAction;
import dk.gormkrings.action.Passive;
import dk.gormkrings.action.Withdraw;
import dk.gormkrings.dto.PhaseRequest;
import dk.gormkrings.dto.SimulationRequest;
import dk.gormkrings.factory.IDateFactory;
import dk.gormkrings.factory.IDepositPhaseFactory;
import dk.gormkrings.factory.IPassivePhaseFactory;
import dk.gormkrings.factory.ISimulationFactory;
import dk.gormkrings.factory.ISpecificationFactory;
import dk.gormkrings.factory.IWithdrawPhaseFactory;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.result.IRunResult;
import dk.gormkrings.simulation.util.ConcurrentCsvExporter;
import dk.gormkrings.statistics.SimulationAggregationService;
import dk.gormkrings.statistics.YearlySummary;
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
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/simulation")
public class FirecastingController {

    private final ISimulationFactory simulationFactory;
    private final IDateFactory dateFactory;
    private final IDepositPhaseFactory depositPhaseFactory;
    private final IPassivePhaseFactory passivePhaseFactory;
    private final IWithdrawPhaseFactory withdrawPhaseFactory;
    private final ISpecificationFactory specificationFactory;
    private final SimulationAggregationService aggregationService;

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
                                 IDepositPhaseFactory depositPhaseFactory,
                                 IPassivePhaseFactory passivePhaseFactory,
                                 IWithdrawPhaseFactory withdrawPhaseFactory,
                                 ISpecificationFactory specificationFactory,
                                 SimulationAggregationService aggregationService) {
        this.simulationFactory = simulationFactory;
        this.dateFactory = dateFactory;
        this.depositPhaseFactory = depositPhaseFactory;
        this.passivePhaseFactory = passivePhaseFactory;
        this.withdrawPhaseFactory = withdrawPhaseFactory;
        this.specificationFactory = specificationFactory;
        this.aggregationService = aggregationService;
    }

    /**
     * POST endpoint to start the simulation.
     * It returns a simulation ID that the frontend can use to subscribe for progress updates.
     */
    @PostMapping("/start")
    public ResponseEntity<String> startSimulation(@RequestBody SimulationRequest request) {
        // Generate a unique simulation ID.
        String simulationId = UUID.randomUUID().toString();
        log.info("Starting simulation with id: {}", simulationId);

        // Build the simulation specification.
        var specification = specificationFactory.newSpecification(
                request.getEpochDay(), request.getTaxPercentage(), request.getReturnPercentage(), 2f);

        var currentDate = dateFactory.dateOf(
                request.getStartDate().getYear(),
                request.getStartDate().getMonth(),
                request.getStartDate().getDayOfMonth());

        List<IPhase> phases = new LinkedList<>();

        for (PhaseRequest pr : request.getPhases()) {
            long days = currentDate.daysUntil(currentDate.plusMonths(pr.getDurationInMonths()));
            IPhase phase = switch (pr.getPhaseType().toUpperCase()) {
                case "DEPOSIT" -> {
                    IAction deposit = new Deposit(pr.getInitialDeposit(), pr.getMonthlyDeposit());
                    yield depositPhaseFactory.createDepositPhase(specification, currentDate, days, deposit);
                }
                case "PASSIVE" -> {
                    IAction passive = new Passive();
                    yield passivePhaseFactory.createPassivePhase(specification, currentDate, days, passive);
                }
                case "WITHDRAW" -> {
                    double withdrawAmount = pr.getWithdrawAmount() != null ? pr.getWithdrawAmount() : 0;
                    IAction withdraw = new Withdraw(withdrawAmount, pr.getWithdrawRate(), 0.5);
                    yield withdrawPhaseFactory.createWithdrawPhase(specification, currentDate, days, withdraw);
                }
                default -> throw new IllegalArgumentException("Unknown phase type: " + pr.getPhaseType());
            };
            phases.add(phase);
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

                // Save results for potential export.
                lastResults = simulationResults;

                // Once simulation finishes, aggregate the results.
                List<YearlySummary> summaries = aggregationService.aggregateResults(
                        simulationResults,
                        progressMessage -> emitterSend(progressMessage, simulationId));
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

    /**
     * GET endpoint to subscribe to simulation progress updates.
     * The frontend calls this using the simulation ID returned from /start.
     */
    @GetMapping("/progress/{simulationId}")
    public SseEmitter getProgress(@PathVariable String simulationId) {
        SseEmitter emitter = new SseEmitter(timeout);
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
