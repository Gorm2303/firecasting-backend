package dk.gormkrings.simulation.simulations;

import dk.gormkrings.simulation.data.Result;
import dk.gormkrings.simulation.engine.schedule.Schedule;
import dk.gormkrings.simulation.engine.schedule.ScheduleEngine;
import dk.gormkrings.simulation.phases.normal.Phase;
import dk.gormkrings.simulation.specification.Spec;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class ScheduleMCSimulation implements Simulation {

    private final ScheduleEngine scheduleEngine;
    private final List<Result> results = new ArrayList<>();

    private final ExecutorService executorService = Executors.newFixedThreadPool(32);

    public ScheduleMCSimulation(ScheduleEngine scheduleEngine) {
        this.scheduleEngine = scheduleEngine;
    }

    public List<Result> run(long runs, List<Phase> phases) {
        results.clear();

        Schedule schedule = scheduleEngine.buildSchedule(phases);


        List<Future<Result>> futures = new ArrayList<>();

        for (int i = 0; i < runs; i++) {
            List<Phase> phaseCopies = new ArrayList<>();
            Spec specification = phases.getFirst().getSpecification().copy();
            for (Phase phase : phases) {
                phaseCopies.add(phase.copy(specification));
            }
            Future<Result> future = executorService.submit(() -> scheduleEngine.simulatePhases(schedule));
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