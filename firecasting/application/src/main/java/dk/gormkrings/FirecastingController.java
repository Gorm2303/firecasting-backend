package dk.gormkrings;

import dk.gormkrings.action.*;
import dk.gormkrings.dto.PhaseRequest;
import dk.gormkrings.dto.SimulationRequest;
import dk.gormkrings.factory.IDateFactory;
import dk.gormkrings.factory.IPhaseFactory;
import dk.gormkrings.factory.ISimulationFactory;
import dk.gormkrings.factory.ISpecificationFactory;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.queue.SimulationQueueService;
import dk.gormkrings.result.IRunResult;
import dk.gormkrings.simulation.util.ConcurrentCsvExporter;
import dk.gormkrings.statistics.SimulationAggregationService;
import dk.gormkrings.statistics.StatisticsService;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

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
    private final SimulationQueueService simQueue;
    private final ScheduledExecutorService sseScheduler;
    private final StatisticsService statisticsService;

    @Value("${settings.runs}")
    private int runs;
    @Value("${settings.batch-size}")
    private int batchSize;
    @Value("${settings.timeout}")
    private long timeout = 60000;

    // Field to store the simulation results for later export.
    private List<IRunResult> lastResults;

    // Map to store SSE emitters by simulation ID.
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public FirecastingController(ISimulationFactory simulationFactory,
                                 IDateFactory dateFactory,
                                 IPhaseFactory phaseFactory,
                                 ISpecificationFactory specificationFactory,
                                 SimulationAggregationService aggregationService,
                                 StatisticsService  statisticsService,
                                 ITaxRuleFactory taxRuleFactory,
                                 ITaxExemptionFactory taxExemptionFactory,
                                 IActionFactory actionFactory,
                                 SimulationQueueService simQueue,
                                 ScheduledExecutorService sseScheduler) {
        this.simulationFactory = simulationFactory;
        this.dateFactory = dateFactory;
        this.phaseFactory = phaseFactory;
        this.specificationFactory = specificationFactory;
        this.aggregationService = aggregationService;
        this.taxRuleFactory = taxRuleFactory;
        this.taxExemptionFactory = taxExemptionFactory;
        this.actionFactory = actionFactory;
        this.simQueue = simQueue;
        this.sseScheduler = sseScheduler;
        this.statisticsService = statisticsService;
    }

    @GetMapping("/schema/simulation")
    public List<UISchemaField> getSimulationSchema() {
        return UISchemaGenerator.generateSchema(SimulationRequest.class);
    }

    @GetMapping("/schema/phase")
    public List<UISchemaField> getPhaseSchema() {
        return UISchemaGenerator.generateSchema(PhaseRequest.class);
    }

    // FirecastingController.java
    @GetMapping("/queue/{simulationId}")
    public ResponseEntity<dk.gormkrings.queue.SimulationQueueService.TaskInfo> queueInfo(@PathVariable String simulationId) {
        var info = simQueue.info(simulationId);
        return ResponseEntity.ok(info);
    }

    @PostMapping("/start")
    public ResponseEntity<String> startSimulation(@RequestBody SimulationRequest request) {
        // 0) If an identical run already exists, return it and bypass the queue.
        var existingId = statisticsService.findExistingRunIdForInput(request);
        if (existingId.isPresent()) {
            log.info("Dedup hit: reusing existing simulation {}", existingId.get());
            return ResponseEntity.ok(existingId.get());
        }

        // Generate a unique simulation ID.
        String simulationId = UUID.randomUUID().toString();
        log.info("Starting simulation with id: {} - {}", simulationId, request.getOverallTaxRule());

        float taxPercentage = request.getTaxPercentage();
        ITaxRule overAllTaxRule = taxRuleFactory.create(request.getOverallTaxRule(), taxPercentage);
        // Build the simulation specification.
        var specification = specificationFactory.create(
                request.getEpochDay(), overAllTaxRule, 1.02D);

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

        // Run the simulation asynchronously (after dedup and phases built)
        boolean accepted = simQueue.submitWithId(simulationId, () -> {
            try {
                var simulationResults = simulationFactory.createSimulation().runWithProgress(
                        runs, batchSize, phases, msg -> emitterSend(msg, simulationId));

                lastResults = simulationResults;

                var summaries = aggregationService.aggregateResults(
                        simulationResults, simulationId, msg -> emitterSend(msg, simulationId));

                var grids = aggregationService.buildPercentileGrids(simulationResults);

                statisticsService.upsertRunWithSummaries(simulationId, request, summaries, grids);

                // Notify all subscribers
                broadcastEvent(simulationId, SseEmitter.event()
                        .name("completed")
                        .data(summaries, MediaType.APPLICATION_JSON));

                // Close all emitters for this id
                var list = emitters.remove(simulationId);
                if (list != null) {
                    for (var em : list) {
                        try { em.complete(); } catch (Exception ignore) {}
                    }
                }

            } catch (Exception e) {
                // Broadcast error + close all emitters for this id
                var list = emitters.remove(simulationId);
                if (list != null) {
                    for (var em : list) {
                        try { em.completeWithError(e); } catch (Exception ignore) {}
                    }
                }
                // Re-throw so queue marks FAILED (if you track it)
                throw new RuntimeException(e);
            }
        });

        // If you haven't already handled it above:
        if (!accepted) {
            return ResponseEntity.status(429).body("Queue full. Try again later.");
        }

        // Return the simulation ID so the frontend can subscribe to progress.
        return ResponseEntity.ok(simulationId);
    }

    private void addEmitter(String id, SseEmitter e) {
        emitters.computeIfAbsent(id, __ -> new CopyOnWriteArrayList<>()).add(e);
        e.onCompletion(() -> removeEmitter(id, e));
        e.onTimeout(() -> removeEmitter(id, e));
    }

    private void removeEmitter(String id, SseEmitter e) {
        var list = emitters.get(id);
        if (list != null) list.remove(e);
    }

    private void broadcast(String id, Object data, MediaType type) {
        var list = emitters.get(id);
        if (list == null) return;
        for (var e : list) {
            try { e.send(data, type); } catch (Exception ignore) {}
        }
    }

    private void broadcastEvent(String id, SseEmitter.SseEventBuilder event) {
        var list = emitters.get(id);
        if (list == null) return;
        for (var e : list) {
            try { e.send(event); } catch (Exception ignore) {}
        }
    }

    // Update your existing progress helper to fan out:
    private void emitterSend(String progressMessage, String simulationId) {
        broadcast(simulationId, progressMessage, MediaType.TEXT_PLAIN);
    }

    @GetMapping(value = "/progress/{simulationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<YearlySummary>> getProgressJson(@PathVariable String simulationId) {
        if (!statisticsService.hasCompletedSummaries(simulationId)) {
            return ResponseEntity.notFound().build();
        }
        var summaries = statisticsService.getSummariesForRun(simulationId);
        return summaries.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(summaries);
    }

    @GetMapping(value = "/progress/{simulationId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getProgressSse(@PathVariable String simulationId) {
        if (statisticsService.hasCompletedSummaries(simulationId)) {
            var emitter = new SseEmitter(0L);
            try {
                var summaries = statisticsService.getSummariesForRun(simulationId);
                emitter.send(SseEmitter.event().name("completed").data(summaries, MediaType.APPLICATION_JSON));
                emitter.complete();
            } catch (Exception e) { emitter.completeWithError(e); }
            return emitter;
        }

        var emitter = new SseEmitter(0L);
        addEmitter(simulationId, emitter);

        // schedule periodic queue updates while QUEUED
        final var handle = new java.util.concurrent.atomic.AtomicReference<java.util.concurrent.ScheduledFuture<?>>();
        Runnable tick = () -> {
            try {
                var info = simQueue.info(simulationId);
                if (info.getStatus() == dk.gormkrings.queue.SimulationQueueService.Status.QUEUED) {
                    emitter.send(SseEmitter.event().name("queued").data(
                            info.getPosition() == null ? "queued" : ("position:" + info.getPosition())));
                } else if (info.getStatus() == dk.gormkrings.queue.SimulationQueueService.Status.RUNNING) {
                    emitter.send(SseEmitter.event().name("started").data("running"));
                    var h = handle.get(); if (h != null) h.cancel(false); // stop queue updates
                } else if (info.getStatus() == dk.gormkrings.queue.SimulationQueueService.Status.DONE) {
                    var h = handle.get(); if (h != null) h.cancel(false);
                    // don't complete here; completion will send 'completed' with summaries
                }
            } catch (Exception ignored) {}
        };
        handle.set(sseScheduler.scheduleAtFixedRate(tick, 0, 5, java.util.concurrent.TimeUnit.SECONDS));

        emitter.onCompletion(() -> { emitters.remove(simulationId); var h = handle.get(); if (h != null) h.cancel(false); });
        emitter.onTimeout(() -> { emitters.remove(simulationId); var h = handle.get(); if (h != null) h.cancel(false); });

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
