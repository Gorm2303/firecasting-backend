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

@Slf4j
@Service
@Scope("prototype")
public class MonteCarloSimulation implements ISimulation {

    private final IEngine engine;
    private final ExecutorService workerPool;
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
        log.info("Selected engine: {} (available: {})", engineName, engines.keySet());
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
        long completed = 0;

        try {
            for (long offset = 0; offset < runs; offset += batchSize) {
                final int thisBatch = (int) Math.min(batchSize, runs - offset);

                // Build tasks for this batch
                List<Callable<IRunResult>> tasks = new ArrayList<>(thisBatch);
                for (int j = 0; j < thisBatch; j++) {
                    // fresh copies for isolation
                    ISpecification specCopy = phases.getFirst().getSpecification().copy();
                    List<IPhase> phaseCopies = new ArrayList<>(phases.size());
                    for (IPhase p : phases) phaseCopies.add(p.copy(specCopy));

                    tasks.add(() -> {
                        try {
                            return engine.simulatePhases(phaseCopies);
                        } catch (Throwable t) {
                            if (failOnTaskError) {
                                throw t; // let ExecutionException propagate
                            } else {
                                log.error("Simulation task failed (run skipped): {}", t.toString(), t);
                                return null; // resilient: skip this run
                            }
                        }
                    });
                }

                // SUBMIT + CONSUME AS THEY COMPLETE (no barrier)
                ExecutorCompletionService<IRunResult> ecs = new ExecutorCompletionService<>(workerPool);
                for (Callable<IRunResult> task : tasks) {
                    ecs.submit(task);
                }

                for (int i = 0; i < thisBatch; i++) {
                    try {
                        Future<IRunResult> f = ecs.take(); // blocks until one task completes
                        IRunResult r = f.get();            // may throw in strict mode
                        if (r == null) continue;           // skip failed task (resilient mode)

                        allResults.add(r);
                        completed++;

                        if (cb != null && (completed % progressStep == 0 || completed == runs)) {
                            long secs = (System.currentTimeMillis() - t0) / 1000;
                            cb.update(String.format("Completed %,d/%,d runs in %,ds", completed, runs, secs));
                        }
                    } catch (ExecutionException ee) {
                        // Only in strict mode; abort entire run
                        log.error("Simulation task failed (strict mode abort)", ee.getCause());
                        throw new RuntimeException("Simulation task failed", ee.getCause());
                    }
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("Simulation interrupted after {} runs", completed);
            throw new RuntimeException("Simulation interrupted", ie);
        }

        log.info("Monte Carlo finished: {}/{} runs in {} ms",
                allResults.size(), runs, System.currentTimeMillis() - t0);
        return allResults;
    }
}
