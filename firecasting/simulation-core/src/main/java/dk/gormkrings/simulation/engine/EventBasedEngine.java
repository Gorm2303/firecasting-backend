package dk.gormkrings.simulation.engine;

import dk.gormkrings.data.Live;
import dk.gormkrings.event.RunEvent;
import dk.gormkrings.event.Type;
import dk.gormkrings.event.date.DayEvent;
import dk.gormkrings.event.date.MonthEvent;
import dk.gormkrings.event.date.YearEvent;
import dk.gormkrings.simulation.EventDispatcher;
import dk.gormkrings.simulation.data.Result;
import dk.gormkrings.simulation.data.Snapshot;
import dk.gormkrings.simulation.phases.eventbased.EPhase;
import dk.gormkrings.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class EventBasedEngine {

    public Result simulatePhases(List<EPhase> phases) {
        Result result = new Result();
        for (EPhase phase : phases) {
            result.addResult(simulatePhase(phase));
        }
        return result;
    }

    private Result simulatePhase(EPhase phase) {
        log.debug("Simulation running for {} days", phase.getDuration());
        Result result = new Result();
        Live data = phase.getLiveData();
        Date startDate = phase.getStartDate();
        EventDispatcher dispatcher = new EventDispatcher(new SimpleApplicationEventMulticaster());
        dispatcher.register(phase);

        result.addSnapshot(new Snapshot(data));

        RunEvent simStart = new RunEvent(this, data, Type.START);
        dispatcher.notifyListeners(simStart);

        // Precompute boundaries using epoch day values.
        int currentEpochDay = startDate.getEpochDay() - 1;
        final int startEpochDay = currentEpochDay;
        // Compute final epoch day from phase duration.
        int finalEpochDay = (int) (startEpochDay + phase.getDuration());

        int nextMonthStartEpochDay = computeNextMonthStart(startDate);
        int currentMonthEndEpochDay = computeMonthEnd(startDate);
        int nextYearStartEpochDay = computeNextYearStart(startDate);
        int currentYearEndEpochDay = computeYearEnd(startDate);

        // Create reusable event objects.
        DayEvent dayEvent = new DayEvent(this, data);
        MonthEvent monthEventStart = new MonthEvent(this, data, Type.START);
        MonthEvent monthEventEnd = new MonthEvent(this, data, Type.END);
        YearEvent yearEventStart = new YearEvent(this, data, Type.START);
        YearEvent yearEventEnd = new YearEvent(this, data, Type.END);

        // Main simulation loop – controlled by epoch day.
        while (currentEpochDay < finalEpochDay) {
            data.incrementTime();
            currentEpochDay++; // advance one day

            // Publish Day Event.
            //dispatcher.notifyListeners(dayEvent);

            // Publish Month Start Event when the current day equals the precomputed boundary.
            if (currentEpochDay == nextMonthStartEpochDay && currentEpochDay != startEpochDay) {
                dispatcher.notifyListeners(monthEventStart);
                // Update boundary for next month start.
                Date newCurrentDate = new Date(currentEpochDay);
                nextMonthStartEpochDay = computeNextMonthStart(newCurrentDate);
            }

            // Publish Month End Event.
            if (currentEpochDay == currentMonthEndEpochDay) {
                dispatcher.notifyListeners(monthEventEnd);
                // Update boundary for month end.
                Date nextDay = new Date(currentEpochDay + 1);
                currentMonthEndEpochDay = computeMonthEnd(nextDay);
            }

            // Publish Year Start Event.
            if (currentEpochDay == nextYearStartEpochDay && currentEpochDay != startEpochDay) {
                dispatcher.notifyListeners(yearEventStart);
                Date newCurrentDate = new Date(currentEpochDay);
                nextYearStartEpochDay = computeNextYearStart(newCurrentDate);
            }

            // Publish Year End Event.
            if (currentEpochDay == currentYearEndEpochDay) {
                dispatcher.notifyListeners(yearEventEnd);
                Date nextDay = new Date(currentEpochDay + 1);
                currentYearEndEpochDay = computeYearEnd(nextDay);
            }
        }

        RunEvent simEnd = new RunEvent(this, data, Type.END);
        dispatcher.notifyListeners(simEnd);

        result.addSnapshot(new Snapshot(data));
        data.resetSession();
        dispatcher.clearRegistrations();
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

