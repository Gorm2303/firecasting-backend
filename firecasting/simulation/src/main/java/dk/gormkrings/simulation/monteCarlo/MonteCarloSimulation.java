package dk.gormkrings.simulation.monteCarlo;

import dk.gormkrings.engine.IEngine;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.result.IResult;
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
    private final List<IResult> results = new ArrayList<>();
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

    public List<IResult> run(long runs, List<IPhase> phases) {
        if (phases.isEmpty() || runs < 0) throw new IllegalArgumentException("No phases to run") ;
        results.clear();
        engine.init(phases);

        List<Future<IResult>> futures = new ArrayList<>();

        for (int i = 0; i < runs; i++) {
            List<IPhase> phaseCopies = new ArrayList<>();
            ISpecification specification = phases.getFirst().getSpecification().copy();
            for (IPhase phase : phases) {
                phaseCopies.add(phase.copy(specification));
            }
            Future<IResult> future = executorService.submit(() -> engine.simulatePhases(phaseCopies));
            futures.add(future);
        }

        for (Future<IResult> future : futures) {
            try {
                IResult result = future.get();
                results.add(result);
            } catch (InterruptedException | ExecutionException e) {
                log.info("Some simulation runs failed: {} result(s), {} run(s)", results.size(), runs);
                log.debug("Error during simulation run", e);
            }
        }

        log.info("Completed simulation runs: {}/{} result(s)", results.size(), runs);
        return results;
    }
}
