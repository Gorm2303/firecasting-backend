package dk.gormkrings.simulation.simulations;

import dk.gormkrings.simulation.Engine;
import dk.gormkrings.simulation.data.Result;
import dk.gormkrings.simulation.phases.Phase;
import dk.gormkrings.simulation.specification.Spec;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

@Service
public class MonteCarloSimulation {

    private final Engine engine;
    private final List<Result> results = new CopyOnWriteArrayList<>();

    private final ExecutorService executorService = Executors.newFixedThreadPool(128);

    public MonteCarloSimulation(Engine engine) {
        this.engine = engine;
    }

    public List<Result> runMonteCarlo(int runs, List<Phase> phases) {
        results.clear();

        List<Future<Result>> futures = new LinkedList<>();

        for (int i = 0; i < runs; i++) {
            List<Phase> phaseCopies = new LinkedList<>();
            Spec specification = phases.getFirst().getSpecification().copy();
            for (Phase phase : phases) {
                phaseCopies.add(phase.copy(specification));
            }

            Future<Result> future = executorService.submit(() -> engine.simulatePhases(phaseCopies));
            futures.add(future);
        }

        for (Future<Result> future : futures) {
            try {
                Result result = future.get();
                results.add(result);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        return results;
    }
}
