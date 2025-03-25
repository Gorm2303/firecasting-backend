package dk.gormkrings.simulation.engine;

import dk.gormkrings.data.Live;
import dk.gormkrings.simulation.data.Result;
import dk.gormkrings.simulation.data.Snapshot;
import dk.gormkrings.simulation.phases.normal.Phase;
import dk.gormkrings.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class Engine {

    public Result simulatePhases(List<Phase> phases) {
        Result result = new Result();
        for (Phase phase : phases) {
            result.addResult(simulatePhase(phase));
        }
        return result;
    }

    private Result simulatePhase(Phase phase) {
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

        int nextMonthStartEpochDay = computeNextMonthStart(startDate);
        int currentMonthEndEpochDay = computeMonthEnd(startDate);
        int nextYearStartEpochDay = computeNextYearStart(startDate);
        int currentYearEndEpochDay = computeYearEnd(startDate);

        // Sim init methods here

        // Main simulation loop – controlled by epoch day.
        while (currentEpochDay < finalEpochDay) {
            data.incrementTime();
            currentEpochDay++; // advance one day

            // Publish Day Event.
            //dispatcher.notifyListeners(dayEvent);

            // Publish Month Start Event when the current day equals the precomputed boundary.
            if (currentEpochDay == nextMonthStartEpochDay && currentEpochDay != startEpochDay) {
                phase.onMonthStart();
                // Update boundary for next month start.
                Date newCurrentDate = new Date(currentEpochDay);
                nextMonthStartEpochDay = computeNextMonthStart(newCurrentDate);
            }

            // Publish Month End Event.
            if (currentEpochDay == currentMonthEndEpochDay) {
                phase.onMonthEnd();
                // Update boundary for month end.
                Date nextDay = new Date(currentEpochDay + 1);
                currentMonthEndEpochDay = computeMonthEnd(nextDay);
            }

            // Publish Year Start Event.
            if (currentEpochDay == nextYearStartEpochDay && currentEpochDay != startEpochDay) {
                phase.onYearStart();
                Date newCurrentDate = new Date(currentEpochDay);
                nextYearStartEpochDay = computeNextYearStart(newCurrentDate);
            }

            // Publish Year End Event.
            if (currentEpochDay == currentYearEndEpochDay) {
                phase.onYearEnd();
                Date nextDay = new Date(currentEpochDay + 1);
                currentYearEndEpochDay = computeYearEnd(nextDay);
            }
        }

        result.addSnapshot(new Snapshot(data));
        data.resetSession();
        return result;
    }

    private int computeNextMonthStart(Date currentDate) {
        int year = currentDate.getYear();
        int month = currentDate.getMonth();
        if (month == 12) {
            return Date.of(year + 1, 1, 1).getEpochDay();
        } else {
            return Date.of(year, month + 1, 1).getEpochDay();
        }
    }

    private int computeMonthEnd(Date currentDate) {
        int year = currentDate.getYear();
        int month = currentDate.getMonth();
        int day = currentDate.lengthOfMonth();
        return Date.of(year, month, day).getEpochDay();
    }

    private int computeNextYearStart(Date currentDate) {
        int year = currentDate.getYear();
        return Date.of(year + 1, 1, 1).getEpochDay();
    }

    private int computeYearEnd(Date currentDate) {
        int year = currentDate.getYear();
        return Date.of(year, 12, 31).getEpochDay();
    }
}
