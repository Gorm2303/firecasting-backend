package dk.gormkrings.simulation.engine;

import dk.gormkrings.data.Live;
import dk.gormkrings.simulation.data.Result;
import dk.gormkrings.simulation.data.Snapshot;
import dk.gormkrings.simulation.phases.Phase;
import dk.gormkrings.simulation.phases.callBased.CallPhase;
import dk.gormkrings.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class CallEngine implements Engine {

    public Result simulatePhases(List<Phase> phaseCopies) {
        Result result = new Result();
        for (Phase phase : phaseCopies) {
            result.addResult(simulatePhase((CallPhase) phase));
        }
        return result;
    }

    private Result simulatePhase(CallPhase phase) {
        log.debug("Simulation running for {} days", phase.getDuration());
        Result result = new Result();
        Live data = phase.getLiveData();
        Date startDate = phase.getStartDate();

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
            phase.onDay();

            // Call Month Start Methods.
            if (currentEpochDay == nextMonthStartEpochDay && currentEpochDay != startEpochDay) {
                phase.onMonthStart();
                // Update boundary for next month start.
                Date newCurrentDate = new Date(currentEpochDay);
                nextMonthStartEpochDay = newCurrentDate.computeNextMonthStart();
            }

            // Call Month End Methods.
            if (currentEpochDay == currentMonthEndEpochDay) {
                phase.onMonthEnd();
                // Update boundary for month end.
                Date nextDay = new Date(currentEpochDay + 1);
                currentMonthEndEpochDay = nextDay.computeMonthEnd();
            }

            // Call Year Start Methods.
            if (currentEpochDay == nextYearStartEpochDay && currentEpochDay != startEpochDay) {
                phase.onYearStart();
                Date newCurrentDate = new Date(currentEpochDay);
                nextYearStartEpochDay = newCurrentDate.computeNextYearStart();
            }

            // Call Year End Methods.
            if (currentEpochDay == currentYearEndEpochDay) {
                phase.onYearEnd();
                Date nextDay = new Date(currentEpochDay + 1);
                currentYearEndEpochDay = nextDay.computeYearEnd();
            }
        }

        result.addSnapshot(new Snapshot(data));
        data.resetSession();
        return result;
    }
}
