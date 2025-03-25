package dk.gormkrings.simulation.engine.schedule;

import dk.gormkrings.simulation.data.Result;
import dk.gormkrings.simulation.phases.normal.Phase;
import dk.gormkrings.util.Date;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ScheduleEngine {

    public Result simulatePhases(Schedule schedule) {
        schedule.execute();
        return new Result();
    }

    public Schedule buildSchedule(List<Phase> phases) {
        List<Event> events = new ArrayList<>();
        for (Phase phase : phases) {
            events.addAll(buildSchedule(phase));
        }
        return new Schedule(events);
    }

    private List<Event> buildSchedule(Phase phase) {
        List<Event> events = new ArrayList<>();
        Date startDate = phase.getStartDate();
        int currentEpochDay = startDate.getEpochDay() - 1;
        final int startEpochDay = currentEpochDay;
        int finalEpochDay = startEpochDay + (int) phase.getDuration();

        int nextMonthStartEpochDay = startDate.computeNextMonthStart();
        int currentMonthEndEpochDay = startDate.computeMonthEnd();
        int nextYearStartEpochDay = startDate.computeNextYearStart();
        int currentYearEndEpochDay = startDate.computeYearEnd();

        // Loop through each day in the simulation schedule.
        while (currentEpochDay < finalEpochDay) {
            currentEpochDay++; // move to next day

            // Always add a day event.
            events.add(new Event(currentEpochDay, phase::onDay));

            if (currentEpochDay == nextMonthStartEpochDay && currentEpochDay != startEpochDay) {
                events.add(new Event(currentEpochDay, phase::onMonthStart));
                Date newDate = new Date(currentEpochDay);
                nextMonthStartEpochDay = newDate.computeNextMonthStart();
            }
            if (currentEpochDay == currentMonthEndEpochDay) {
                events.add(new Event(currentEpochDay, phase::onMonthEnd));
                Date nextDay = new Date(currentEpochDay + 1);
                currentMonthEndEpochDay = nextDay.computeMonthEnd();
            }
            if (currentEpochDay == nextYearStartEpochDay && currentEpochDay != startEpochDay) {
                events.add(new Event(currentEpochDay, phase::onYearStart));
                Date newDate = new Date(currentEpochDay);
                nextYearStartEpochDay = newDate.computeNextYearStart();
            }
            if (currentEpochDay == currentYearEndEpochDay) {
                events.add(new Event(currentEpochDay, phase::onYearEnd));
                Date nextDay = new Date(currentEpochDay + 1);
                currentYearEndEpochDay = nextDay.computeYearEnd();
            }
        }
        return events;
    }
}
