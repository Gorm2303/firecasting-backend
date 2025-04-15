package dk.gormkrings.simulation.monteCarlo;

import dk.gormkrings.engine.IEngine;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.result.IRunResult;
import dk.gormkrings.simulation.IProgressCallback;
import dk.gormkrings.simulation.ISimulation;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.updates.IProgressService;
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
import java.util.logging.Formatter;

@Slf4j
@Service
@Scope("prototype")
public class MonteCarloSimulation implements ISimulation {

    private final IEngine engine;
    private final List<IRunResult> results = new ArrayList<>();
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
        return runWithProgress(runs, phases, null);
    }

    public List<IRunResult> runWithProgress(long runs, List<IPhase> phases, IProgressCallback callback) {
        if (phases.isEmpty() || runs < 0) throw new IllegalArgumentException("No phases to run");
        results.clear();
        engine.init(phases);

        List<Future<IRunResult>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < runs; i++) {
            List<IPhase> phaseCopies = new ArrayList<>();
            ISpecification specification = phases.getFirst().getSpecification().copy();
            for (IPhase phase : phases) {
                phaseCopies.add(phase.copy(specification));
            }
            Future<IRunResult> future = executorService.submit(() -> engine.simulatePhases(phaseCopies));
            futures.add(future);
        }
        for (Future<IRunResult> future : futures) {
            try {
                IRunResult result = future.get();
                results.add(result);
                if (results.size() % 1000 == 0) {
                    long blockEndTime = System.currentTimeMillis();

                    String progressMessage = String.format("Completed %,d/%,d runs in %,ds",
                            results.size(), runs,
                            (blockEndTime - startTime)/1000);
                    log.info(progressMessage);
                    // Invoke the progress callback.
                    callback.update(progressMessage);
                }
            } catch (InterruptedException | ExecutionException e) {
                log.info("Some simulation runs failed: {} result(s), {} run(s)", results.size(), runs);
                log.debug("Error during simulation run", e);
            }
        }

        log.info("Handled simulation runs in: {} ms", System.currentTimeMillis() - startTime);
        log.info("Completed simulation runs: {}/{} result(s)", results.size(), runs);
        return results;
    }
}
