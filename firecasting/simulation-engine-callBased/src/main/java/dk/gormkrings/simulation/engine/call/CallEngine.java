package dk.gormkrings.simulation.engine.call;

import dk.gormkrings.data.IDate;
import dk.gormkrings.data.ILiveData;
import dk.gormkrings.engine.IEngine;
import dk.gormkrings.factory.IDateFactory;
import dk.gormkrings.factory.IResultFactory;
import dk.gormkrings.factory.ISnapshotFactory;
import dk.gormkrings.phase.ICallPhase;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.result.IResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service("callEngine")
public class CallEngine implements IEngine {

    private final IDateFactory dateFactory;
    private final IResultFactory resultFactory;
    private final ISnapshotFactory snapshotFactory;

    public CallEngine(IDateFactory dateFactory, IResultFactory resultFactory, ISnapshotFactory snapshotFactory) {
        this.dateFactory = dateFactory;
        this.resultFactory = resultFactory;
        this.snapshotFactory = snapshotFactory;
    }

    public IResult simulatePhases(List<IPhase> phaseCopies) {
        IResult result = resultFactory.newResult();
        result.addSnapshot(snapshotFactory.snapshot((ILiveData) phaseCopies.getFirst().getLiveData()));
        for (IPhase phase : phaseCopies) {
            result.addResult(simulatePhase((ICallPhase) phase));
        }
        return result;
    }

    private IResult simulatePhase(ICallPhase phase) {
        log.debug("Simulation running for {} days", phase.getDuration());
        IResult result = resultFactory.newResult();
        ILiveData data = (ILiveData) phase.getLiveData();
        IDate startDate = phase.getStartDate();

        // Precompute boundaries using epoch day values.
        int currentEpochDay = startDate.getEpochDay() - 1;
        final int startEpochDay = currentEpochDay;
        // Compute final epoch day from phase duration.
        int finalEpochDay = (int) (startEpochDay + phase.getDuration());

        int nextMonthStartEpochDay = startDate.computeNextMonthStart();
        int currentMonthEndEpochDay = startDate.computeMonthEnd();
        int nextYearStartEpochDay = startDate.computeNextYearStart();
        int currentYearEndEpochDay = startDate.computeYearEnd();

        // Sim init methods here

        // Main simulation loop – controlled by epoch day.
        while (currentEpochDay < finalEpochDay) {
            data.incrementTime();
            currentEpochDay++; // advance one day

            // Call Day Methods.
            //phase.onDayStart();

            // Call Month Start Methods.
            if (currentEpochDay == nextMonthStartEpochDay && currentEpochDay != startEpochDay) {
                phase.onMonthStart();
                // Update boundary for next month start.
                IDate newCurrentDate = dateFactory.fromEpochDay(currentEpochDay);
                nextMonthStartEpochDay = newCurrentDate.computeNextMonthStart();
            }

            // Call Month End Methods.
            if (currentEpochDay == currentMonthEndEpochDay) {
                phase.onMonthEnd();
                // Update boundary for month end.
                IDate nextDay = dateFactory.fromEpochDay(currentEpochDay);
                currentMonthEndEpochDay = nextDay.computeNextMonthEnd();
            }

            // Call Year Start Methods.
            if (currentEpochDay == nextYearStartEpochDay && currentEpochDay != startEpochDay) {
                phase.onYearStart();
                IDate newCurrentDate = dateFactory.fromEpochDay(currentEpochDay);
                nextYearStartEpochDay = newCurrentDate.computeNextYearStart();
            }

            // Call Year End Methods.
            if (currentEpochDay == currentYearEndEpochDay) {
                phase.onYearEnd();
                IDate nextDay = dateFactory.fromEpochDay(currentEpochDay);
                currentYearEndEpochDay = nextDay.computeNextYearEnd();
            }
        }

        result.addSnapshot(snapshotFactory.snapshot(data));
        data.resetSession();
        return result;
    }

    @Override
    public void init(List<IPhase> phases) {

    }
}
