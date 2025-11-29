package dk.gormkrings;

import dk.gormkrings.dto.PhaseRequest;
import dk.gormkrings.dto.SimulationRequest;
import dk.gormkrings.queue.SimulationQueueService;
import dk.gormkrings.result.IRunResult;
import dk.gormkrings.simulation.SimulationRunner;
import dk.gormkrings.simulation.util.ConcurrentCsvExporter;
import dk.gormkrings.statistics.StatisticsService;
import dk.gormkrings.statistics.YearlySummary;
import dk.gormkrings.sse.SimulationSseService;
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

    private final SimulationQueueService simQueue;
    private final SimulationRunner simulationRunner;
    private final SimulationSseService sseService;
    private final StatisticsService statisticsService;
    private final ScheduledExecutorService sseScheduler;

    @Value("${settings.runs}")
    private int runs;

    @Value("${settings.batch-size}")
    private int batchSize;

    @Value("${settings.timeout}")
    private long timeout;

    @Value("${simulation.progressStep:1000}")
    private int progressStep;

    private List<IRunResult> lastestResults;

    public FirecastingController(SimulationQueueService simQueue,
                                 SimulationRunner simulationRunner,
                                 SimulationSseService sseService,
                                 StatisticsService statisticsService,
                                 ScheduledExecutorService sseScheduler) {
        this.simQueue = simQueue;
        this.simulationRunner = simulationRunner;
        this.sseService = sseService;
        this.statisticsService = statisticsService;
        this.sseScheduler = sseScheduler;
    }

    // ------------------------------------------------------------------------------------
    // Schema endpoints (unchanged)
    // ------------------------------------------------------------------------------------

    @GetMapping("/schema/simulation")
    public List<UISchemaField> getSimulationSchema() {
        return UISchemaGenerator.generateSchema(SimulationRequest.class);
    }

    @GetMapping("/schema/phase")
    public List<UISchemaField> getPhaseSchema() {
        return UISchemaGenerator.generateSchema(PhaseRequest.class);
    }

    // ------------------------------------------------------------------------------------
    // Queue info
    // ------------------------------------------------------------------------------------

    @GetMapping("/queue/{simulationId}")
    public ResponseEntity<SimulationQueueService.TaskInfo> queueInfo(@PathVariable String simulationId) {
        var info = simQueue.info(simulationId);
        return ResponseEntity.ok(info);
    }

    // ------------------------------------------------------------------------------------
    // Start simulation
    // ------------------------------------------------------------------------------------

    @PostMapping(
            value = "/start",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, String>> startSimulation(@Valid @RequestBody SimulationRequest request) {
        // 0) Dedup FIRST, return immediately if hit
        try {
            var existingId = statisticsService.findExistingRunIdForInput(request);
            if (existingId.isPresent()) {
                log.info("[/start] Dedup hit -> {}", existingId.get());
                return ResponseEntity.ok(Map.of("id", existingId.get()));
            }
            log.info("[/start] Dedup miss -> creating new run");
        } catch (Exception e) {
            log.error("[/start] Dedup check failed", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid input for dedup: " + e.getMessage()));
        }

        // 1) Simple invariant: max duration
        int totalMonths = request.getPhases().stream()
                .mapToInt(PhaseRequest::getDurationInMonths)
                .sum();
        if (totalMonths > 1200) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "Total duration across phases must be ≤ 1200 months (got " + totalMonths + ")"));
        }

        // 2) New run id, enqueue job, respond 202 with JSON {id}
        final String simulationId = UUID.randomUUID().toString();
        log.info("[/start] New run -> {} - {}", simulationId, request.getOverallTaxRule());

        boolean accepted = simQueue.submitWithId(simulationId, () -> {
            try {
                // Start SSE flushing pipeline
                sseService.startFlusher(simulationId);

                // Run the simulation via SimulationRunner

                lastestResults = simulationRunner.runSimulation(
                        simulationId,
                        request,
                        runs,
                        batchSize,
                        msg -> sseService.onProgressMessage(simulationId, msg)
                );

                // Fetch summaries from DB and emit "completed" with data
                var summaries = statisticsService.getSummariesForRun(simulationId);
                sseService.sendCompleted(simulationId, summaries);
            } catch (Exception e) {
                log.error("Simulation failed {}", simulationId, e);
                sseService.stopFlusherWithError(simulationId, e);
            } finally {
                // Ensure flusher and SSE emitters are cleaned up
                sseService.stopFlusherAndComplete(simulationId);
            }
        });

        if (!accepted) {
            return ResponseEntity.status(429)
                    .body(Map.of("error", "Queue full. Try again later."));
        }

        sseService.enqueueStateQueued(simulationId, "queued");
        return ResponseEntity.accepted().body(Map.of("id", simulationId));
    }

    // ------------------------------------------------------------------------------------
    // Progress – JSON (completed summaries from DB)
    // ------------------------------------------------------------------------------------

    @GetMapping(value = "/progress/{simulationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<YearlySummary>> getProgressJson(@PathVariable String simulationId) {
        if (!statisticsService.hasCompletedSummaries(simulationId)) {
            return ResponseEntity.notFound().build();
        }
        var summaries = statisticsService.getSummariesForRun(simulationId);
        return summaries.isEmpty()
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(summaries);
    }

    // ------------------------------------------------------------------------------------
    // Progress – SSE stream
    // ------------------------------------------------------------------------------------

    @GetMapping(value = "/progress/{simulationId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getProgressSse(@PathVariable String simulationId) {
        // If summaries already exist -> send "completed" immediately and close stream
        if (statisticsService.hasCompletedSummaries(simulationId)) {
            var emitter = new SseEmitter(0L);
            try {
                var summaries = statisticsService.getSummariesForRun(simulationId);
                emitter.send(SseEmitter.event()
                        .name("completed")
                        .data(summaries, MediaType.APPLICATION_JSON));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        var emitter = new SseEmitter(0L);
        sseService.addEmitter(simulationId, emitter);

        try {
            emitter.send(SseEmitter.event().name("open").data("ok"));
        } catch (Exception ignore) {
        }

        final AtomicReference<ScheduledFuture<?>> handle = new AtomicReference<>();

        Runnable tick = () -> {
            try {
                var info = simQueue.info(simulationId);
                if (info == null || info.getStatus() == null) {
                    if (!"QUEUED".equals(sseService.getLastState(simulationId))) {
                        sseService.enqueueStateQueued(simulationId, "queued");
                        sseService.setLastState(simulationId, "QUEUED");
                    }
                    return;
                }

                String status = info.getStatus().name();
                switch (info.getStatus()) {
                    case QUEUED -> {
                        Integer p = info.getPosition(); // already 0-based
                        Integer prev = sseService.getLastQueuedPos(simulationId);

                        if (p != null) {
                            if (!Objects.equals(prev, p)) {
                                sseService.enqueueStateQueued(simulationId, "position:" + p);
                                sseService.setLastQueuedPos(simulationId, p);
                            }
                        } else {
                            if (!Objects.equals(prev, -1)) {
                                sseService.enqueueStateQueued(simulationId, "queued");
                                sseService.setLastQueuedPos(simulationId, -1);
                            }
                        }

                        if (!"QUEUED".equals(sseService.getLastState(simulationId))) {
                            sseService.setLastState(simulationId, "QUEUED");
                        }
                    }
                    case RUNNING -> {
                        if (!"RUNNING".equals(sseService.getLastState(simulationId))) {
                            sseService.enqueueStateStarted(simulationId, "running");
                            sseService.setLastState(simulationId, "RUNNING");
                        }
                        sseService.setLastQueuedPos(simulationId, null);
                        sseService.enqueueHeartbeat(simulationId);
                    }
                    case DONE, FAILED -> {
                        sseService.setLastState(simulationId, status);
                        sseService.setLastQueuedPos(simulationId, null);
                        var h = handle.get();
                        if (h != null) h.cancel(false);
                    }
                }
            } catch (Exception ignored) {
            }
        };

        handle.set(sseScheduler.scheduleAtFixedRate(tick, 0, 2, TimeUnit.SECONDS));

        emitter.onCompletion(() -> {
            sseService.clearState(simulationId);
            var h = handle.get();
            if (h != null) h.cancel(false);
        });

        emitter.onTimeout(() -> {
            sseService.clearState(simulationId);
            var h = handle.get();
            if (h != null) h.cancel(false);
        });

        return emitter;
    }

    // ------------------------------------------------------------------------------------
    // Export last results as CSV
    // ------------------------------------------------------------------------------------

    @GetMapping("/export")
    public ResponseEntity<StreamingResponseBody> exportResultsAsCsv() {
        if (lastestResults == null || lastestResults.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        long simTime = System.currentTimeMillis();
        File file;
        try {
            file = ConcurrentCsvExporter.exportCsv(lastestResults, "simulation-results");
        } catch (IOException e) {
            log.error("Error exporting CSV", e);
            return ResponseEntity.status(500)
                    .body(outputStream -> outputStream.write("Error exporting CSV".getBytes()));
        }
        long exportTime = System.currentTimeMillis();
        log.info("Handling exports in {} ms", exportTime - simTime);

        StreamingResponseBody stream = out -> {
            Files.copy(file.toPath(), out);
            out.flush();
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getName() + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(stream);
    }
}
