package dk.gormkrings.simulation.monteCarlo;

import dk.gormkrings.engine.IEngine;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.result.IResult;
import dk.gormkrings.simulation.ISimulation;
import dk.gormkrings.specification.ISpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Service
public class MonteCarloSimulation implements ISimulation {

    private final IEngine engine;
    private final List<IResult> results = new ArrayList<>();

    private final ExecutorService executorService = Executors.newFixedThreadPool(32);

    public MonteCarloSimulation(IEngine engine) {
        this.engine = engine;
        log.debug("Initializing Monte Carlo Simulation");
    }

    public List<IResult> run(long runs, List<IPhase> phases) {
        results.clear();

        List<Future<IResult>> futures = new ArrayList<>();

        for (int i = 0; i < runs; i++) {
            List<IPhase> phaseCopies = new ArrayList<>();
            ISpec specification = phases.getFirst().getSpecification().copy();
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
                e.printStackTrace();
            }
        }

        return results;
    }
}
