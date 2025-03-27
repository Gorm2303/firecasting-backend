package dk.gormkrings.simulation.engine.call;

import dk.gormkrings.data.IDate;
import dk.gormkrings.data.ILiveData;
import dk.gormkrings.engine.IEngine;
import dk.gormkrings.phase.ICallPhase;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.result.IResult;
import dk.gormkrings.simulation.data.Date;
import dk.gormkrings.simulation.result.Result;
import dk.gormkrings.simulation.result.Snapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class CallEngine implements IEngine {

    public IResult simulatePhases(List<IPhase> phaseCopies) {
        IResult result = new Result();
        for (IPhase phase : phaseCopies) {
            result.addResult(simulatePhase((ICallPhase) phase));
        }
        return result;
    }

    private IResult simulatePhase(ICallPhase phase) {
        log.debug("Simulation running for {} days", phase.getDuration());
        IResult result = new Result();
        ILiveData data = (ILiveData) phase.getLiveData();
        IDate startDate = phase.getStartDate();

        result.addSnapshot(new Snapshot(data));

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
            phase.onDayStart();

            // Call Month Start Methods.
            if (currentEpochDay == nextMonthStartEpochDay && currentEpochDay != startEpochDay) {
                phase.onMonthStart();
                // Update boundary for next month start.
                IDate newCurrentDate = new Date(currentEpochDay);
                nextMonthStartEpochDay = newCurrentDate.computeNextMonthStart();
            }

            // Call Month End Methods.
            if (currentEpochDay == currentMonthEndEpochDay) {
                phase.onMonthEnd();
                // Update boundary for month end.
                IDate nextDay = new Date(currentEpochDay);
                currentMonthEndEpochDay = nextDay.computeNextMonthEnd();
            }

            // Call Year Start Methods.
            if (currentEpochDay == nextYearStartEpochDay && currentEpochDay != startEpochDay) {
                phase.onYearStart();
                IDate newCurrentDate = new Date(currentEpochDay);
                nextYearStartEpochDay = newCurrentDate.computeNextYearStart();
            }

            // Call Year End Methods.
            if (currentEpochDay == currentYearEndEpochDay) {
                phase.onYearEnd();
                IDate nextDay = new Date(currentEpochDay);
                currentYearEndEpochDay = nextDay.computeNextYearEnd();
            }
        }

        result.addSnapshot(new Snapshot(data));
        data.resetSession();
        return result;
    }
}
