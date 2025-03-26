package dk.gormkrings.simulation.engine.event;

import dk.gormkrings.data.Live;
import dk.gormkrings.event.RunEvent;
import dk.gormkrings.event.Type;
import dk.gormkrings.event.date.DayEvent;
import dk.gormkrings.event.date.MonthEvent;
import dk.gormkrings.event.date.YearEvent;
import dk.gormkrings.simulation.data.Result;
import dk.gormkrings.simulation.data.Snapshot;
import dk.gormkrings.simulation.engine.Engine;
import dk.gormkrings.simulation.phases.Phase;
import dk.gormkrings.simulation.phases.eventbased.EventPhase;
import dk.gormkrings.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class EventBasedEngine implements Engine {

    public Result simulatePhases(List<Phase> phaseCopies) {
        Result result = new Result();
        for (Phase phase : phaseCopies) {
            result.addResult(simulatePhase((EventPhase) phase));
        }
        return result;
    }

    private Result simulatePhase(EventPhase phase) {
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

        int nextMonthStartEpochDay = startDate.computeNextMonthStart();
        int currentMonthEndEpochDay = startDate.computeMonthEnd();
        int nextYearStartEpochDay = startDate.computeNextYearStart();
        int currentYearEndEpochDay = startDate.computeYearEnd();

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
                nextMonthStartEpochDay = newCurrentDate.computeNextMonthStart();
            }

            // Publish Month End Event.
            if (currentEpochDay == currentMonthEndEpochDay) {
                dispatcher.notifyListeners(monthEventEnd);
                // Update boundary for month end.
                Date nextDay = new Date(currentEpochDay + 1);
                currentMonthEndEpochDay = nextDay.computeMonthEnd();
            }

            // Publish Year Start Event.
            if (currentEpochDay == nextYearStartEpochDay && currentEpochDay != startEpochDay) {
                dispatcher.notifyListeners(yearEventStart);
                Date newCurrentDate = new Date(currentEpochDay);
                nextYearStartEpochDay = newCurrentDate.computeNextYearStart();
            }

            // Publish Year End Event.
            if (currentEpochDay == currentYearEndEpochDay) {
                dispatcher.notifyListeners(yearEventEnd);
                Date nextDay = new Date(currentEpochDay + 1);
                currentYearEndEpochDay = nextDay.computeYearEnd();
            }
        }

        RunEvent simEnd = new RunEvent(this, data, Type.END);
        dispatcher.notifyListeners(simEnd);

        result.addSnapshot(new Snapshot(data));
        data.resetSession();
        dispatcher.clearRegistrations();
        return result;
    }
}

