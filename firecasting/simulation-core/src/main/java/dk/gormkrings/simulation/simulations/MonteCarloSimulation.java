package dk.gormkrings.simulation.simulations;

import dk.gormkrings.simulation.Engine;
import dk.gormkrings.simulation.data.Result;
import dk.gormkrings.simulation.phases.Phase;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class MonteCarloSimulation {

    private final Engine engine;
    private final List<Result> results = new CopyOnWriteArrayList<>();

    public MonteCarloSimulation(Engine engine) {
        this.engine = engine;
    }

    public List<Result> runMonteCarlo(int runs, List<Phase> phases) {
        results.clear();
        results.addAll(
                IntStream.range(0, runs)
                        .parallel() // Parallelize using the common ForkJoinPool; you can also submit tasks to a TaskExecutor.
                        .mapToObj(i -> engine.simulatePhases(phases))
                        .toList()
        );
        return results;
    }
}
