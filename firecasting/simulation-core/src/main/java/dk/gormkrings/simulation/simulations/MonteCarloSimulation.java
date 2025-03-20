package dk.gormkrings.simulation.simulations;

import dk.gormkrings.simulation.Engine;
import dk.gormkrings.simulation.data.Result;
import dk.gormkrings.simulation.phases.Phase;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Service
public class MonteCarloSimulation {

    private final Engine engine;
    // Thread-safe collection for storing simulation results.
    private final List<Result> results = new CopyOnWriteArrayList<>();

    // Create a fixed thread pool; adjust pool size as needed.
    private final ExecutorService executorService = Executors.newFixedThreadPool(8);

    public MonteCarloSimulation(Engine engine) {
        this.engine = engine;
    }

    public List<Result> runMonteCarlo(int runs, List<Phase> phases) {
        // Clear any previous results.
        results.clear();

        // List to hold Futures.
        List<Future<Result>> futures = new ArrayList<>();

        // Use a for loop to submit simulation tasks.
        for (int i = 0; i < runs; i++) {
            // Create a deep copy of the phases for this simulation run.
            List<Phase> phaseCopies = new ArrayList<>();
            Phase previousPhase = null;
            for (Phase phase : phases) {
                Phase phaseCopy = phase.copy(previousPhase);
                phaseCopies.add(phaseCopy);
                previousPhase = phaseCopy;
            }

            // Submit the simulation task and collect the Future.
            Future<Result> future = executorService.submit(() -> engine.simulatePhases(phaseCopies));
            futures.add(future);
        }

        // Wait for all tasks to complete and collect the results.
        for (Future<Result> future : futures) {
            try {
                Result result = future.get(); // Blocks until the task is complete.
                results.add(result);
            } catch (InterruptedException | ExecutionException e) {
                // Handle exceptions appropriately.
                e.printStackTrace();
            }
        }

        return results;
    }
}
