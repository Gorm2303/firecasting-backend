package dk.gormkrings.simulation.monteCarlo;

import dk.gormkrings.engine.schedule.IScheduleEngine;
import dk.gormkrings.phase.ICallPhase;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.result.IResult;
import dk.gormkrings.simulation.ISimulation;
import dk.gormkrings.specification.ISpec;
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
public class ScheduleMCSimulation implements ISimulation {

    private final IScheduleEngine scheduleEngine;
    private final List<IResult> results = new ArrayList<>();

    private final ExecutorService executorService = Executors.newFixedThreadPool(32);

    public ScheduleMCSimulation(IScheduleEngine scheduleEngine) {
        this.scheduleEngine = scheduleEngine;
        log.debug("Initializing Schedule Monte Carlo Simulation");

    }

    public List<IResult> run(long runs, List<IPhase> phases) {
        results.clear();

        List<Future<IResult>> futures = new ArrayList<>();

        for (int i = 0; i < runs; i++) {
            List<ICallPhase> phaseCopies = new ArrayList<>();
            ISpec specification = phases.getFirst().getSpecification().copy();
            for (IPhase phase : phases) {
                phaseCopies.add((ICallPhase) phase.copy(specification));
            }
            Future<IResult> future = executorService.submit(() -> scheduleEngine.simulatePhases(phaseCopies));
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