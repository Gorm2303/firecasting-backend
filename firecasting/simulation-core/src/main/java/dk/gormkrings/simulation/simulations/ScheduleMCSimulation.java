package dk.gormkrings.simulation.simulations;

import dk.gormkrings.simulation.results.Result;
import dk.gormkrings.simulation.engine.schedule.Schedule;
import dk.gormkrings.simulation.engine.schedule.ScheduleBuilder;
import dk.gormkrings.simulation.engine.schedule.ScheduleEngine;
import dk.gormkrings.simulation.phases.Phase;
import dk.gormkrings.simulation.specification.Spec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@Service
public class ScheduleMCSimulation implements Simulation {

    private final ScheduleEngine scheduleEngine;
    private final List<Result> results = new ArrayList<>();

    private final ExecutorService executorService = Executors.newFixedThreadPool(32);

    public ScheduleMCSimulation(ScheduleEngine scheduleEngine) {
        this.scheduleEngine = scheduleEngine;
        log.debug("Initializing Schedule Monte Carlo Simulation");
    }

    public List<Result> run(long runs, List<Phase> phases) {
        results.clear();

        ScheduleBuilder scheduleBuilder = new ScheduleBuilder();
        Schedule schedule = scheduleBuilder.buildSchedule(phases);
        scheduleEngine.setSchedule(schedule);

        List<Future<Result>> futures = new ArrayList<>();

        for (int i = 0; i < runs; i++) {
            List<Phase> phaseCopies = new ArrayList<>();
            Spec specification = phases.getFirst().getSpecification().copy();
            for (Phase phase : phases) {
                phaseCopies.add(phase.copy(specification));
            }
            Future<Result> future = executorService.submit(() -> scheduleEngine.simulatePhases(phaseCopies));
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