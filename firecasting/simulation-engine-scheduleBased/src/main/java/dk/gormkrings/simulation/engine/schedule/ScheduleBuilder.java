package dk.gormkrings.simulation.engine.schedule;

import dk.gormkrings.data.IDate;
import dk.gormkrings.event.EventType;
import dk.gormkrings.event.IEvent;
import dk.gormkrings.phase.ICallPhase;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.simulation.data.Date;
import dk.gormkrings.simulation.event.Event;

import java.util.ArrayList;
import java.util.List;

public class ScheduleBuilder {
    private int nextWeekStartEpoch;
    private int weekEndEpoch;
    private int nextMonthStartEpoch;
    private int monthEndEpoch;
    private int nextYearStartEpoch;
    private int yearEndEpoch;
    private int finalEpoch;
    private int currentEpoch;
    private List<IEvent> events;

    public Schedule buildSchedule(List<IPhase> phases) {
        events = new ArrayList<>();
        for (IPhase phase : phases) {
            buildSchedule((ICallPhase) phase);
            int lastEpochDay = (int) (phase.getStartDate().getEpochDay() + phase.getDuration());
            events.add(new Event(lastEpochDay, EventType.PHASE_SWITCH));
        }
        return new Schedule(events);
    }

    private void initPhase(IPhase phase) {
        IDate startDate = phase.getStartDate();
        int startEpoch = startDate.getEpochDay();
        finalEpoch = startEpoch + (int) phase.getDuration();
        currentEpoch = startEpoch - 1;

        // Precompute boundaries using the Date instance.
        nextWeekStartEpoch  = startDate.computeNextWeekStart();
        weekEndEpoch        = startDate.computeWeekEnd();
        nextMonthStartEpoch = startDate.computeNextMonthStart();
        monthEndEpoch       = startDate.computeMonthEnd();
        nextYearStartEpoch  = startDate.computeNextYearStart();
        yearEndEpoch        = startDate.computeYearEnd();
    }

    private void buildSchedule(ICallPhase phase) {
        initPhase(phase);

        // Precompute Checks.
        boolean supportDayStart = phase.supportsEvent(EventType.DAY_START);
        boolean supportDayEnd   = phase.supportsEvent(EventType.DAY_END);
        boolean supportWeekStart= phase.supportsEvent(EventType.WEEK_START);
        boolean supportWeekEnd  = phase.supportsEvent(EventType.WEEK_END);
        boolean supportMonthStart = phase.supportsEvent(EventType.MONTH_START);
        boolean supportMonthEnd   = phase.supportsEvent(EventType.MONTH_END);
        boolean supportYearStart  = phase.supportsEvent(EventType.YEAR_START);
        boolean supportYearEnd    = phase.supportsEvent(EventType.YEAR_END);

        // Loop over each day in the phase.
        while (currentEpoch < finalEpoch) {
            currentEpoch++; // Advance one day.

            checkDayStartEvent(supportDayStart);
            checkDayEndEvent(supportDayEnd);
            checkWeekStartEvent(supportWeekStart);
            checkWeekEndEvent(supportWeekEnd);
            checkMonthStartEvent(supportMonthStart);
            checkMonthEndEvent(supportMonthEnd);
            checkYearStartEvent(supportYearStart);
            checkYearEndEvent(supportYearEnd);
        }
    }

    private void checkYearEndEvent(boolean supportYearEnd) {
        // Check for YEAR_END event.
        if (currentEpoch != yearEndEpoch || !supportYearEnd) return;

        addEvent(EventType.YEAR_END);
        IDate nextDay = new Date(currentEpoch);
        yearEndEpoch = nextDay.computeNextYearEnd();
    }

    private void checkYearStartEvent(boolean supportYearStart) {
        // Check for YEAR_START event.
        if (currentEpoch != nextYearStartEpoch || !supportYearStart) return;

        addEvent(EventType.YEAR_START);
        IDate newDate = new Date(currentEpoch);
        nextYearStartEpoch = newDate.computeNextYearStart();
    }

    private void checkMonthEndEvent(boolean supportMonthEnd) {
        // Check for MONTH_END event.
        if (currentEpoch != monthEndEpoch || !supportMonthEnd) return;

        addEvent(EventType.MONTH_END);
        IDate nextDay = new Date(currentEpoch);
        monthEndEpoch = nextDay.computeNextMonthEnd();
    }

    private void checkMonthStartEvent(boolean supportMonthStart) {
        // Check for MONTH_START event.
        if (currentEpoch != nextMonthStartEpoch || !supportMonthStart) return;

        addEvent(EventType.MONTH_START);
        IDate newDate = new Date(currentEpoch);
        nextMonthStartEpoch = newDate.computeNextMonthStart();
    }

    private void checkWeekEndEvent(boolean supportWeekEnd) {
        // Check for WEEK_END event.
        if (currentEpoch != weekEndEpoch || !supportWeekEnd) return;

        addEvent(EventType.WEEK_END);
        IDate nextDay = new Date(currentEpoch);
        weekEndEpoch = nextDay.computeNextWeekEnd();
    }

    private void checkWeekStartEvent(boolean supportWeekStart) {
        // Check for WEEK_START event.
        if (currentEpoch != nextWeekStartEpoch || !supportWeekStart) return;

        addEvent(EventType.WEEK_START);
        IDate newDate = new Date(currentEpoch);
        this.nextWeekStartEpoch = newDate.computeNextWeekStart();
    }

    private void checkDayEndEvent(boolean supportDayEnd) {
        // Check for DAY_END event.
        if (!supportDayEnd) return;
        addEvent(EventType.DAY_END);
    }

    private void checkDayStartEvent(boolean supportDayStart) {
        // Check for DAY_START event.
        if (!supportDayStart) return;
        addEvent(EventType.DAY_START);
    }

    private void addEvent(EventType eventType) {
        events.add(new Event(currentEpoch, eventType));
    }
}
