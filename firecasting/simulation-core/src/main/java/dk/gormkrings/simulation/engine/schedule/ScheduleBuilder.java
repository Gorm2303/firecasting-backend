package dk.gormkrings.simulation.engine.schedule;

import dk.gormkrings.simulation.phases.Phase;
import dk.gormkrings.simulation.phases.callBased.CallPhase;
import dk.gormkrings.util.Date;

import java.util.ArrayList;
import java.util.List;

public class ScheduleBuilder {

    public Schedule buildSchedule(List<Phase> phases) {
        List<Event> events = new ArrayList<>();
        for (Phase phase : phases) {
            List<Event> phaseEvents = buildSchedule((CallPhase) phase);
            events.addAll(phaseEvents);
            int lastEpochDay = (int) (phase.getStartDate().getEpochDay() + phase.getDuration());
            events.add(new Event(lastEpochDay, EventType.PHASE_SWITCH));
        }
        return new Schedule(events);
    }

    private List<Event> buildSchedule(CallPhase phase) {
        List<Event> events = new ArrayList<>();
        Date startDate = phase.getStartDate();
        int startEpoch = startDate.getEpochDay();
        int finalEpoch = startEpoch + (int) phase.getDuration();
        int currentEpoch = startEpoch - 1;

        // Precompute boundaries using the Date instance.
        int nextMonthStartEpoch = startDate.computeNextMonthStart();
        int currentMonthEndEpoch = startDate.computeMonthEnd();
        int nextYearStartEpoch = startDate.computeNextYearStart();
        int currentYearEndEpoch = startDate.computeYearEnd();

        // Loop over each day in the phase.
        while (currentEpoch < finalEpoch) {
            currentEpoch++; // Advance one day.

            // Add a DAY event.
            events.add(new Event(currentEpoch, EventType.DAY_START));

            // Check for month start event.
            if (currentEpoch == nextMonthStartEpoch && currentEpoch != startEpoch) {
                events.add(new Event(currentEpoch, EventType.MONTH_START));
                Date newDate = new Date(currentEpoch);
                nextMonthStartEpoch = newDate.computeNextMonthStart();
            }

            // Check for month end event.
            if (currentEpoch == currentMonthEndEpoch) {
                events.add(new Event(currentEpoch, EventType.MONTH_END));
                Date nextDay = new Date(currentEpoch + 1);
                currentMonthEndEpoch = nextDay.computeMonthEnd();
            }

            // Check for year start event.
            if (currentEpoch == nextYearStartEpoch && currentEpoch != startEpoch) {
                events.add(new Event(currentEpoch, EventType.YEAR_START));
                Date newDate = new Date(currentEpoch);
                nextYearStartEpoch = newDate.computeNextYearStart();
            }

            // Check for year end event.
            if (currentEpoch == currentYearEndEpoch) {
                events.add(new Event(currentEpoch, EventType.YEAR_END));
                Date nextDay = new Date(currentEpoch + 1);
                currentYearEndEpoch = nextDay.computeYearEnd();
            }
        }
        return events;
    }
}
