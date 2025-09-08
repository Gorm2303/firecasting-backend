package dk.gormkrings.simulation.monteCarlo;

import dk.gormkrings.engine.IEngine;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.result.IRunResult;
import dk.gormkrings.simulation.IProgressCallback;
import dk.gormkrings.simulation.ISimulation;
import dk.gormkrings.specification.ISpecification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@Scope("prototype")
public class MonteCarloSimulation implements ISimulation {

    private final IEngine engine;
    private final ExecutorService workerPool;

    /** Emit a progress update each time this many runs complete globally. */
    private final int progressStep;

    /** If true, a task failure aborts the whole run (handy for tests). Default false for resilience. */
    private final boolean failOnTaskError;

    public MonteCarloSimulation(
            Map<String, IEngine> engines,
            @Value("${simulation.engine.selected:scheduleEngine}") String engineName,
            @Qualifier("simWorkerPool") ExecutorService workerPool,
            @Value("${simulation.progressStep:1000}") int progressStep,
            @Value("${simulation.failOnTaskError:false}") boolean failOnTaskError
    ) {
        if (!engines.containsKey(engineName)) {
            throw new IllegalArgumentException("No engine found with name: " + engineName +
                    ". Available engines: " + engines.keySet());
        }
        this.engine = engines.get(engineName);
        this.workerPool = workerPool;
        this.progressStep = Math.max(1, progressStep);
        this.failOnTaskError = failOnTaskError;
        log.info("Selected engine: {} (available: {}), progressStep={}", engineName, engines.keySet(), this.progressStep);
    }

    @Override
    public List<IRunResult> run(long runs, List<IPhase> phases) {
        return runWithProgress(runs, 10_000, phases, null);
    }

    @Override
    public List<IRunResult> runWithProgress(long runs, int batchSize, List<IPhase> phases, IProgressCallback cb) {
        if (phases == null || phases.isEmpty() || runs <= 0) {
            throw new IllegalArgumentException("No phases to run or runs <= 0");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be > 0");
        }

        engine.init(phases);

        final List<IRunResult> allResults = new ArrayList<>((int) Math.min(runs, Integer.MAX_VALUE));
        final long t0 = System.currentTimeMillis();

        // Global counters shared across worker tasks
        final AtomicLong completed = new AtomicLong(0);
        final AtomicLong nextMilestone = new AtomicLong(Math.min(progressStep, runs));

        // Determine effective parallelism from the pool if possible
        final int cores = (workerPool instanceof ThreadPoolExecutor tpe)
                ? Math.max(1, tpe.getCorePoolSize())
                : Math.max(1, Runtime.getRuntime().availableProcessors());

        try {
            for (long offset = 0; offset < runs; offset += batchSize) {
                final int thisBatch = (int) Math.min(batchSize, runs - offset);

                // Build a small number of "fat" tasks; each task runs many paths and reports progress as it goes
                final List<Callable<List<IRunResult>>> tasks = buildTasks(phases, cores, thisBatch, runs,
                        completed, nextMilestone, cb, t0);

                // Submit and wait for the batch to finish
                final List<Future<List<IRunResult>>> futures = workerPool.invokeAll(tasks);

                for (Future<List<IRunResult>> f : futures) {
                    try {
                        final List<IRunResult> chunk = f.get(); // may throw only in strict mode
                        if (chunk != null && !chunk.isEmpty()) {
                            allResults.addAll(chunk);
                        }
                    } catch (ExecutionException ee) {
                        // Only happens in strict mode; abort entire run
                        log.error("Simulation task failed (strict mode abort)", ee.getCause());
                        throw new RuntimeException("Simulation task failed", ee.getCause());
                    }
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("Simulation interrupted after {} runs", completed.get());
            throw new RuntimeException("Simulation interrupted", ie);
        }

        // Ensure a final "Completed runs/runs" is emitted if we didn't land exactly on a milestone
        long done = completed.get();
        if (cb != null && done < runs) {
            cb.update(String.format("Completed %,d/%,d runs", runs, runs));
        }

        log.info("Monte Carlo finished: {}/{} runs in {} ms",
                allResults.size(), runs, System.currentTimeMillis() - t0);
        return allResults;
    }

    private List<Callable<List<IRunResult>>> buildTasks(
            List<IPhase> phases,
            int cores,
            int thisBatch,
            long totalRuns,
            AtomicLong completed,
            AtomicLong nextMilestone,
            IProgressCallback cb,
            long t0
    ) {
        final int taskCount   = Math.min(cores, thisBatch);
        final int basePerTask = thisBatch / taskCount;
        final int remainder   = thisBatch % taskCount;

        List<Callable<List<IRunResult>>> tasks = new ArrayList<>(taskCount);
        for (int ti = 0; ti < taskCount; ti++) {
            final int quota = basePerTask + (ti < remainder ? 1 : 0);

            tasks.add(() -> {
                List<IRunResult> chunk = new ArrayList<>(quota);

                for (int k = 0; k < quota; k++) {
                    // fresh copies per run for isolation
                    ISpecification specCopy = phases.getFirst().getSpecification().copy();
                    List<IPhase> phaseCopies = new ArrayList<>(phases.size());
                    for (IPhase p : phases) phaseCopies.add(p.copy(specCopy));

                    try {
                        IRunResult r = engine.simulatePhases(phaseCopies);
                        if (r != null) chunk.add(r);
                    } catch (Throwable t) {
                        if (failOnTaskError) {
                            throw t; // propagate → ExecutionException → abort the whole run
                        } else {
                            log.error("Simulation task failed (run skipped): {}", t.toString(), t);
                            // continue; resilient mode
                        }
                    }

                    // ---- progress every progressStep runs (global) ----
                    long done = completed.incrementAndGet();

                    // Advance milestones one-by-one; only one thread will win each CAS
                    while (true) {
                        long target = nextMilestone.get();
                        if (done < target) break; // not yet at next milestone

                        long newTarget = Math.min(target + progressStep, totalRuns);
                        if (nextMilestone.compareAndSet(target, newTarget)) {
                            if (cb != null) {
                                // Keep the message short; the controller will coalesce & pace
                                cb.update(String.format("Completed %,d/%,d runs", target, totalRuns));
                            }
                            // if many runs just finished at once, loop to potentially emit the next milestone too
                            if (newTarget == target) break; // safety
                        } else {
                            // lost the race; re-check with updated target
                            if (nextMilestone.get() <= done) continue;
                            break;
                        }
                    }
                }

                return chunk;
            });
        }
        return tasks;
    }
}
