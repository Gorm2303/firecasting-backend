package dk.gormkrings.simulation.monteCarlo;

import dk.gormkrings.engine.IEngine;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.result.IRunResult;
import dk.gormkrings.simulation.IProgressCallback;
import dk.gormkrings.simulation.ISimulation;
import dk.gormkrings.specification.ISpecification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@Service
@Scope("prototype")
public class MonteCarloSimulation implements ISimulation {

    private final IEngine engine;
    private final List<IRunResult> allResults = new ArrayList<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(32);

    // Inject all IEngine beans as a map and select one based on a configuration property.
    public MonteCarloSimulation(Map<String, IEngine> engines,
                                @Value("${simulation.engine.selected:scheduleEngine}") String engineName) {
        if (engines.containsKey(engineName)) {
            this.engine = engines.get(engineName);
            log.info("Selected engine: {} from available engines: {}", engineName, engines.keySet());
        } else {
            throw new IllegalArgumentException("No engine found with name: " + engineName +
                    ". Available engines: " + engines.keySet());
        }
    }

    public List<IRunResult> run(long runs, List<IPhase> phases) {
        return runWithProgress(runs, 10000, phases, null);
    }

    public List<IRunResult> runWithProgress(long runs, int batchSize, List<IPhase> phases, IProgressCallback callback) {
        if (phases.isEmpty() || runs < 0) throw new IllegalArgumentException("No phases to run");
        allResults.clear();
        engine.init(phases);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < runs; i += batchSize) {
            List<Future<IRunResult>> futures = new ArrayList<>();
            for (int j = 0; j < batchSize && (i + j) < runs; j++) {
                List<IPhase> phaseCopies = new ArrayList<>();
                ISpecification specification = phases.getFirst().getSpecification().copy();
                for (IPhase phase : phases) {
                    phaseCopies.add(phase.copy(specification));
                }
                futures.add(executorService.submit(() -> engine.simulatePhases(phaseCopies)));
            }

            // Collect results for this batch.
            for (Future<IRunResult> future : futures) {
                try {
                    allResults.add(future.get());
                    if (allResults.size() % 1000 == 0) {
                        long blockEndTime = System.currentTimeMillis();

                        String progressMessage = String.format("Completed %,d/%,d runs in %,ds",
                                allResults.size(), runs,
                                (blockEndTime - startTime) / 1000);
                        log.info(progressMessage);
                        // Invoke the progress callback.
                        if (callback != null) callback.update(progressMessage);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    log.info("Some simulation runs failed: {} result(s), {} run(s)", allResults.size(), runs);
                    log.error("Error in batch simulation", e);
                }
            }

        }

        log.info("Handled simulation runs in: {} ms", System.currentTimeMillis() - startTime);
        log.info("Completed simulation runs: {}/{} result(s)", allResults.size(), runs);
        return allResults;
    }
}
