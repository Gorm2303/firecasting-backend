package dk.gormkrings;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.gormkrings.action.*;
import dk.gormkrings.dto.PhaseRequest;
import dk.gormkrings.dto.SimulationRequest;
import dk.gormkrings.dto.ProgressUpdate; // <-- new DTO below
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
import org.springframework.context.annotation.Profile;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Profile("!local") // only active when NOT in local mode
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

    private final ObjectMapper mapper = new ObjectMapper();

    /** Global SSE pacing (ms). All events spaced by this interval; "completed" bypasses. */
    @Value("${settings.sse-interval:1000}")
    private long sseIntervalMs;

    @Value("${settings.runs}")
    private int runs;
    @Value("${settings.batch-size}")
    private int batchSize;
    @Value("${settings.timeout}")
    private long timeout = 60000;
    @Value("${simulation.progressStep:1000}")
    private int progressStep;

    private List<IRunResult> lastResults;

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    // ---- Throttled/coalesced event pipeline ---------------------------------------------

    private static final int PRIO_HEARTBEAT = -1; // lowest
    private static final int PRIO_PROGRESS  =  0; // numeric run progress
    private static final int PRIO_OTHER     =  1; // normal text messages
    private static final int PRIO_STATE     =  2; // queued/started etc.

    private static final class PendingEvent {
        final String name;
        final Object data;
        final MediaType type;
        final int priority;
        PendingEvent(String name, Object data, MediaType type, int priority) {
            this.name = name; this.data = data; this.type = type; this.priority = priority;
        }
    }

    /** Latest pending event per simulation (coalesced). */
    private final ConcurrentHashMap<String, AtomicReference<PendingEvent>> pendingBySim = new ConcurrentHashMap<>();
    /** Per-simulation flusher task (paces to sseIntervalMs). */
    private final ConcurrentHashMap<String, ScheduledFuture<?>> flusherBySim = new ConcurrentHashMap<>();
    /** Last lifecycle state sent (to avoid duplicate "queued"/"started"). */
    private final ConcurrentHashMap<String, String> lastStateSent = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> lastQueuedPos = new ConcurrentHashMap<>();


    // --------------------------------------------------------------------------------------

    public FirecastingController(ISimulationFactory simulationFactory,
                                 IDateFactory dateFactory,
                                 IPhaseFactory phaseFactory,
                                 ISpecificationFactory specificationFactory,
                                 SimulationAggregationService aggregationService,
                                 StatisticsService statisticsService,
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

    @GetMapping("/queue/{simulationId}")
    public ResponseEntity<SimulationQueueService.TaskInfo> queueInfo(@PathVariable String simulationId) {
        var info = simQueue.info(simulationId);
        return ResponseEntity.ok(info);
    }

    @PostMapping(
            value = "/start",
            produces = MediaType.APPLICATION_JSON_VALUE, // <-- force JSON
            consumes = MediaType.APPLICATION_JSON_VALUE
)
    public ResponseEntity<Map<String,String>> startSimulation(@Valid @RequestBody SimulationRequest request) {
        // --- 0) Dedup FIRST, return immediately if hit ---
        try {
            var existingId = statisticsService.findExistingRunIdForInput(request);
            if (existingId.isPresent()) {
                log.info("[/start] Dedup hit -> {}", existingId.get());
                return ResponseEntity.ok(Map.of("id", existingId.get())); // <-- MUST return
            }
            log.info("[/start] Dedup miss -> creating new run");
        } catch (Exception e) {
            // If something goes wrong during hashing/lookup, return a clean 400 with the message
            log.error("[/start] Dedup check failed", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid input for dedup: " + e.getMessage()));
        }

        // --- 1) Validate simple invariants before any heavy work ---
        int totalMonths = request.getPhases().stream().mapToInt(PhaseRequest::getDurationInMonths).sum();
        if (totalMonths > 1200) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Total duration across phases must be ≤ 1200 months (got " + totalMonths + ")"));
        }

        // --- 2) New run id, enqueue job, respond 202 with JSON {id} ---
        final String simulationId = UUID.randomUUID().toString();
        log.info("[/start] New run -> {} - {}", simulationId, request.getOverallTaxRule());

        boolean accepted = simQueue.submitWithId(simulationId, () -> {
            try {
                startFlusher(simulationId);

                float taxPercentage = request.getTaxPercentage();
                ITaxRule overAllTaxRule = taxRuleFactory.create(request.getOverallTaxRule(), taxPercentage);

                var specification = specificationFactory.create(request.getEpochDay(), overAllTaxRule, "dataDrivenReturn", 1.02D);

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
                                pr.getInitialDeposit(), pr.getMonthlyDeposit(), pr.getYearlyIncreaseInPercentage());
                        case "withdraw" -> actionFactory.createWithdrawAction(
                                pr.getWithdrawAmount(), pr.getWithdrawRate(),
                                pr.getLowerVariationPercentage(), pr.getUpperVariationPercentage());
                        case "passive" -> actionFactory.createPassiveAction();
                        default -> throw new IllegalArgumentException("Unknown phase type: " + phaseType);
                    };

                    phases.add(phaseFactory.create(phaseType, specification, currentDate, taxExemptions, days, action));
                    currentDate = currentDate.plusMonths(pr.getDurationInMonths());
                }

                var simulationResults = simulationFactory.createSimulation().runWithProgress(
                        runs, batchSize, phases, msg -> onProgress(simulationId, msg));

                lastResults = simulationResults;

                var summaries = aggregationService.aggregateResults(
                        simulationResults, simulationId, msg -> onProgress(simulationId, msg));
                var grids = aggregationService.buildPercentileGrids(simulationResults);

                // Pure inserts (no delete/clear)
                statisticsService.insertNewRunWithSummaries(simulationId, request, summaries, grids);

                flushOnce(simulationId);
                broadcastEvent(simulationId, SseEmitter.event().name("completed")
                        .data(summaries, MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                log.error("Simulation failed {}", simulationId, e);
                var list = emitters.remove(simulationId);
                if (list != null) for (var em : list) try { em.completeWithError(e); } catch (Exception ignore) {}
            } finally {
                stopFlusher(simulationId);
                var list = emitters.remove(simulationId);
                if (list != null) for (var em : list) try { em.complete(); } catch (Exception ignore) {}
            }
        });

        if (!accepted) return ResponseEntity.status(429).body(Map.of("error", "Queue full. Try again later."));

        enqueueState(simulationId, "queued", "queued");
        return ResponseEntity.accepted().body(Map.of("id", simulationId));
    }

    /**
     * Accepts either:
     *  - JSON ProgressUpdate ({"kind":"RUNS","completed":N,"total":M} or {"kind":"MESSAGE","message":"..."})
     *  - Plain text (legacy): treated as OTHER "progress".
     */
    private void onProgress(String simulationId, String progressMessage) {
        if (progressMessage == null) return;
        final String msg = progressMessage.trim();
        boolean handled = false;

        if (msg.startsWith("{") && msg.contains("\"kind\"")) {
            try {
                ProgressUpdate pu = mapper.readValue(msg, ProgressUpdate.class);
                if (pu.getKind() == ProgressUpdate.Kind.RUNS) {
                    // keep it terse; UI already labels as "progress"
                    String line = String.format("Completed %,d/%,d runs", pu.getCompleted(), pu.getTotal());
                    enqueue(simulationId, "progress", line, PRIO_PROGRESS);
                    handled = true;
                } else if (pu.getKind() == ProgressUpdate.Kind.MESSAGE && pu.getMessage() != null) {
                    enqueue(simulationId, "progress", pu.getMessage(), PRIO_OTHER);
                    handled = true;
                }
            } catch (Exception ignore) {
                // fall through to legacy text
            }
        }
        if (!handled) {
            enqueue(simulationId, "progress", msg, PRIO_OTHER);
        }
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

    private void broadcastEvent(String id, SseEmitter.SseEventBuilder event) {
        var list = emitters.get(id);
        if (list == null || list.isEmpty()) return;

        List<SseEmitter> dead = null;
        for (var e : list) {
            try { e.send(event); }
            catch (Exception ex) { if (dead == null) dead = new ArrayList<>(); dead.add(e); }
        }
        if (dead != null) list.removeAll(dead);
    }

    @GetMapping(value = "/progress/{simulationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<YearlySummary>> getProgressJson(@PathVariable String simulationId) {
        if (!statisticsService.hasCompletedSummaries(simulationId)) return ResponseEntity.notFound().build();
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

        try { emitter.send(SseEmitter.event().name("open").data("ok")); } catch (Exception ignore) {}

        final var handle = new AtomicReference<ScheduledFuture<?>>();
        Runnable tick = () -> {
            try {
                var info = simQueue.info(simulationId);
                if (info == null || info.getStatus() == null) {
                    if (!"QUEUED".equals(lastStateSent.get(simulationId))) {
                        enqueueState(simulationId, "queued", "queued");
                        lastStateSent.put(simulationId, "QUEUED");
                    }
                    return;
                }
                String status = info.getStatus().name();
                switch (info.getStatus()) {
                    // inside getProgressSse(...) -> tick Runnable -> switch(info.getStatus())
                    case QUEUED -> {
                        // Always compute current position
                        Integer p = info.getPosition(); // already 0-based from SimulationQueueService.info()
                        Integer prev = lastQueuedPos.get(simulationId);

                        if (p != null) {
                            // Send if changed (coalesced by 1s flusher)
                            if (!Objects.equals(prev, p)) {
                                enqueueState(simulationId, "queued", "position:" + p);
                                lastQueuedPos.put(simulationId, p);
                            }
                        } else {
                            // Fallback: unknown position
                            if (!Objects.equals(prev, -1)) {
                                enqueueState(simulationId, "queued", "queued");
                                lastQueuedPos.put(simulationId, -1);
                            }
                        }

                        // Keep lastStateSent only for first-time "QUEUED" transition, if you want:
                        if (!"QUEUED".equals(lastStateSent.get(simulationId))) {
                            lastStateSent.put(simulationId, "QUEUED");
                        }
                    }
                    case RUNNING -> {
                        if (!"RUNNING".equals(lastStateSent.get(simulationId))) {
                            enqueueState(simulationId, "started", "running");
                            lastStateSent.put(simulationId, "RUNNING");
                        }
                        // no longer queued; forget old position
                        lastQueuedPos.remove(simulationId);
                        enqueue(simulationId, "heartbeat", "tick", PRIO_HEARTBEAT);
                    }
                    case DONE, FAILED -> {
                        lastStateSent.put(simulationId, status);
                        lastQueuedPos.remove(simulationId);
                        var h = handle.get(); if (h != null) h.cancel(false);
                    }
                }
            } catch (Exception ignored) {}
        };

        handle.set(sseScheduler.scheduleAtFixedRate(tick, 0, 2, TimeUnit.SECONDS));

        emitter.onCompletion(() -> {
            removeEmitter(simulationId, emitter);
            lastStateSent.remove(simulationId);
            var h = handle.get(); if (h != null) h.cancel(false);
        });
        emitter.onTimeout(() -> {
            removeEmitter(simulationId, emitter);
            lastStateSent.remove(simulationId);
            var h = handle.get(); if (h != null) h.cancel(false);
        });

        return emitter;
    }

    @GetMapping("/export")
    public ResponseEntity<StreamingResponseBody> exportResultsAsCsv() {
        if (lastResults == null || lastResults.isEmpty()) return ResponseEntity.noContent().build();

        long simTime = System.currentTimeMillis();
        File file;
        try {
            file = ConcurrentCsvExporter.exportCsv(lastResults, "simulation-results");
        } catch (IOException e) {
            log.error("Error exporting CSV", e);
            return ResponseEntity.status(500).body(outputStream -> outputStream.write("Error exporting CSV".getBytes()));
        }
        long exportTime = System.currentTimeMillis();
        log.info("Handling exports in {} ms", exportTime - simTime);

        StreamingResponseBody stream = out -> { Files.copy(file.toPath(), out); out.flush(); };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(stream);
    }

    // ===== Throttled / coalesced pipeline ================================================

    private void enqueueState(String simId, String name, Object data) {
        enqueue(simId, name, data, PRIO_STATE);
    }

    private void enqueue(String simId, String name, Object data, int prio) {
        var ref = pendingBySim.computeIfAbsent(simId, __ -> new AtomicReference<>());
        PendingEvent next = new PendingEvent(name, data, MediaType.TEXT_PLAIN, prio);

        while (true) {
            PendingEvent cur = ref.get();
            if (cur == null) {
                if (ref.compareAndSet(null, next)) break;
            } else {
                // Keep higher priority; progress overwrites progress; state beats others
                if (prio < cur.priority) break; // lower prio → ignore
                if (ref.compareAndSet(cur, next)) break;
            }
        }
        startFlusher(simId);
    }

    private void startFlusher(String simId) {
        flusherBySim.computeIfAbsent(simId, id ->
                sseScheduler.scheduleAtFixedRate(() -> flushOnce(id), 0L, sseIntervalMs, TimeUnit.MILLISECONDS)
        );
    }

    private void stopFlusher(String simId) {
        var f = flusherBySim.remove(simId);
        if (f != null) f.cancel(false);
        pendingBySim.remove(simId);
        lastStateSent.remove(simId);
    }

    /** Sends at most one pending event for this sim (paced by scheduler). */
    private void flushOnce(String simId) {
        var ref = pendingBySim.get(simId);
        if (ref == null) return;
        PendingEvent ev = ref.getAndSet(null);
        if (ev == null) return;
        SseEmitter.SseEventBuilder sse = SseEmitter.event().name(ev.name);
        if (ev.data != null) sse = sse.data(ev.data, ev.type);
        broadcastEvent(simId, sse);
    }
}
