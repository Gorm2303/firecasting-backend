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
import jakarta.validation.Valid;
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
    public ResponseEntity<String> startSimulation(
            @Valid @RequestBody SimulationRequest request,
            @RequestParam(value = "simulationId", required = false) String clientId
    ) {
        // 0) Dedup by canonicalized input (fast exit)
        var existingId = statisticsService.findExistingRunIdForInput(request);
        if (existingId.isPresent()) {
            log.info("Dedup hit: reusing existing simulation {}", existingId.get());
            return ResponseEntity.ok(existingId.get());
        }

        // 1) Cross-request guard: total duration across phases ≤ 1200 months
        int totalMonths = request.getPhases().stream()
                .mapToInt(PhaseRequest::getDurationInMonths)
                .sum();
        if (totalMonths > 1200) {
            return ResponseEntity.badRequest()
                    .body("Total duration across phases must be ≤ 1200 months (got " + totalMonths + ")");
        }

        // 2) Use client-provided ID if present so the SSE subscriber matches
        final String simulationId = (clientId != null && !clientId.isBlank())
                ? clientId
                : UUID.randomUUID().toString();
        log.info("Starting simulation with id: {} - {}", simulationId, request.getOverallTaxRule());

        // 3) Build phases/spec
        float taxPercentage = request.getTaxPercentage();
        ITaxRule overAllTaxRule = taxRuleFactory.create(request.getOverallTaxRule(), taxPercentage);

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

        // 4) Enqueue async simulation
        boolean accepted = simQueue.submitWithId(simulationId, () -> {
            try {
                var simulationResults = simulationFactory.createSimulation().runWithProgress(
                        runs, batchSize, phases, msg -> emitterSend(msg, simulationId));

                lastResults = simulationResults;

                var summaries = aggregationService.aggregateResults(
                        simulationResults, simulationId, msg -> emitterSend(msg, simulationId));

                var grids = aggregationService.buildPercentileGrids(simulationResults);

                statisticsService.upsertRunWithSummaries(simulationId, request, summaries, grids);

                // Notify all subscribers with final payload
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
                throw new RuntimeException(e);
            }
        });

        if (!accepted) {
            return ResponseEntity.status(429).body("Queue full. Try again later.");
        }

        // 5) Give immediate visual feedback if a subscriber is already connected
        broadcastEvent(simulationId, SseEmitter.event().name("queued").data("queued"));

        // 6) Return the id the server actually uses (client should switch if different)
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
        // Build once to preserve ordering; schedule the actual send on the SSE thread
        SseEmitter.SseEventBuilder ev = SseEmitter.event()
                .name("progress")
                .data(progressMessage, MediaType.TEXT_PLAIN);

        sseScheduler.execute(() -> broadcastEvent(simulationId, ev));
    }


    @GetMapping(value = "/progress/{simulationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<YearlySummary>> getProgressJson(@PathVariable String simulationId) {
        if (!statisticsService.hasCompletedSummaries(simulationId)) {
            return ResponseEntity.notFound().build();
        }
        var summaries = statisticsService.getSummariesForRun(simulationId);
        return summaries.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(summaries);
    }

    // 2) When a client subscribes, send an opening event and start a lightweight heartbeat.
    //    Keep the heartbeat even while RUNNING; cancel only on completion/timeout.
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

        try {
            // opening ping primes proxies/buffers
            emitter.send(SseEmitter.event().name("open").data("ok"));
        } catch (Exception ignore) {}

        final var handle = new java.util.concurrent.atomic.AtomicReference<java.util.concurrent.ScheduledFuture<?>>();
        Runnable tick = () -> {
            try {
                // inside tick runnable in getProgressSse
                var info = simQueue.info(simulationId);
                if (info == null || info.getStatus() == null) {
                    emitter.send(SseEmitter.event().name("queued").data("queued"));
                    return;
                }


                switch (info.getStatus()) {
                    case QUEUED -> emitter.send(SseEmitter.event().name("queued")
                            .data(info.getPosition() == null ? "queued" : ("position:" + info.getPosition())));
                    case RUNNING -> {
                        // Send a small heartbeat while running to keep intermediaries flushing
                        emitter.send(SseEmitter.event().name("heartbeat").comment("tick"));
                    }
                    case DONE, FAILED -> {
                        var h = handle.get();
                        if (h != null) h.cancel(false);
                    }
                }
            } catch (Exception ignored) {}
        };
        // fire immediately, then every 2s
        handle.set(sseScheduler.scheduleAtFixedRate(tick, 0, 2, java.util.concurrent.TimeUnit.SECONDS));

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
