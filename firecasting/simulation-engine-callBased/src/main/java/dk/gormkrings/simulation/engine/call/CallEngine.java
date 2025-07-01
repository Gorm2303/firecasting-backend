package dk.gormkrings.simulation.engine.call;

import dk.gormkrings.data.IDate;
import dk.gormkrings.data.ILiveData;
import dk.gormkrings.engine.IEngine;
import dk.gormkrings.factory.IDateFactory;
import dk.gormkrings.factory.IResultFactory;
import dk.gormkrings.factory.ISnapshotFactory;
import dk.gormkrings.phase.ICallPhase;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.result.IRunResult;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.List;

@Setter
@Getter
@Slf4j
@Service("callEngine")
@Scope("prototype")
public class CallEngine implements IEngine {

    private IDateFactory dateFactory;
    private IResultFactory resultFactory;
    private ISnapshotFactory snapshotFactory;

    public CallEngine(IDateFactory dateFactory, IResultFactory resultFactory, ISnapshotFactory snapshotFactory) {
        this.dateFactory = dateFactory;
        this.resultFactory = resultFactory;
        this.snapshotFactory = snapshotFactory;
    }

    public IRunResult simulatePhases(List<IPhase> phaseCopies) {
        IRunResult result = resultFactory.newResult();
        for (IPhase phase : phaseCopies) {
            result.addResult(simulatePhase((ICallPhase) phase));
        }
        return result;
    }

    private IRunResult simulatePhase(ICallPhase phase) {
        log.debug("Simulation running for {} days", phase.getDuration());
        IRunResult result = resultFactory.newResult();
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
        phase.onPhaseStart();

        // Main simulation loop – controlled by epoch day.
        while (currentEpochDay < finalEpochDay) {
            phase.getLiveData().incrementTime();
            currentEpochDay++; // advance one day
            phase.onDayStart();

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
                result.addSnapshot(snapshotFactory.snapshot((ILiveData) phase.getLiveData()));
            }
            phase.onDayEnd();
        }
        phase.onPhaseEnd();

        result.addSnapshot(snapshotFactory.snapshot((ILiveData) phase.getLiveData()));
        phase.getLiveData().resetSession();
        return result;
    }

    @Override
    public void init(List<IPhase> phases) {

    }
}
