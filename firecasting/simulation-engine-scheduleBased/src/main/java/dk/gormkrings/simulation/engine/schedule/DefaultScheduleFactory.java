package dk.gormkrings.simulation.engine.schedule;

import dk.gormkrings.data.IDate;
import dk.gormkrings.engine.schedule.ISchedule;
import dk.gormkrings.engine.schedule.IScheduleFactory;
import dk.gormkrings.engine.schedule.IScheduleEvent;
import dk.gormkrings.event.EventType;
import dk.gormkrings.factory.IDateFactory;
import dk.gormkrings.phase.ICallPhase;
import dk.gormkrings.phase.IPhase;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DefaultScheduleFactory implements IScheduleFactory {
    private int nextWeekStartEpoch;
    private int weekEndEpoch;
    private int nextMonthStartEpoch;
    private int monthEndEpoch;
    private int nextYearStartEpoch;
    private int yearEndEpoch;
    private int finalEpoch;
    private int currentEpoch;
    private List<IScheduleEvent> events;
    private final IDateFactory dateFactory;
    private ISchedule schedule;

    public DefaultScheduleFactory(IDateFactory dateFactory) {
        this.dateFactory = dateFactory;
    }

    public ISchedule build(List<IPhase> phases) {
        if (schedule != null) return schedule;
        events = new ArrayList<>();
        for (IPhase phase : phases) {
            buildSchedule((ICallPhase) phase);
            events.add(new ScheduleEvent(phase.getStartDate().getEpochDay(), EventType.PHASE_START));
            int lastEpochDay = (int) (phase.getStartDate().getEpochDay() + phase.getDuration());
            events.add(new ScheduleEvent(lastEpochDay, EventType.PHASE_END));
        }
        schedule = new Schedule(events);
        return schedule;
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
        IDate nextDay = dateFactory.fromEpochDay(currentEpoch);
        yearEndEpoch = nextDay.computeNextYearEnd();
    }

    private void checkYearStartEvent(boolean supportYearStart) {
        // Check for YEAR_START event.
        if (currentEpoch != nextYearStartEpoch || !supportYearStart) return;

        addEvent(EventType.YEAR_START);
        IDate newDate = dateFactory.fromEpochDay(currentEpoch);
        nextYearStartEpoch = newDate.computeNextYearStart();
    }

    private void checkMonthEndEvent(boolean supportMonthEnd) {
        // Check for MONTH_END event.
        if (currentEpoch != monthEndEpoch || !supportMonthEnd) return;

        addEvent(EventType.MONTH_END);
        IDate nextDay = dateFactory.fromEpochDay(currentEpoch);
        monthEndEpoch = nextDay.computeNextMonthEnd();
    }

    private void checkMonthStartEvent(boolean supportMonthStart) {
        // Check for MONTH_START event.
        if (currentEpoch != nextMonthStartEpoch || !supportMonthStart) return;

        addEvent(EventType.MONTH_START);
        IDate newDate = dateFactory.fromEpochDay(currentEpoch);
        nextMonthStartEpoch = newDate.computeNextMonthStart();
    }

    private void checkWeekEndEvent(boolean supportWeekEnd) {
        // Check for WEEK_END event.
        if (currentEpoch != weekEndEpoch || !supportWeekEnd) return;

        addEvent(EventType.WEEK_END);
        IDate nextDay = dateFactory.fromEpochDay(currentEpoch);
        weekEndEpoch = nextDay.computeNextWeekEnd();
    }

    private void checkWeekStartEvent(boolean supportWeekStart) {
        // Check for WEEK_START event.
        if (currentEpoch != nextWeekStartEpoch || !supportWeekStart) return;

        addEvent(EventType.WEEK_START);
        IDate newDate = dateFactory.fromEpochDay(currentEpoch);
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
        events.add(new ScheduleEvent(currentEpoch, eventType));
    }
}
