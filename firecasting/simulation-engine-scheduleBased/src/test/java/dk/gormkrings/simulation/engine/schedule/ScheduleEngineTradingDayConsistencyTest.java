package dk.gormkrings.simulation.engine.schedule;

import dk.gormkrings.calendar.WeekdayTradingCalendar;
import dk.gormkrings.data.IDate;
import dk.gormkrings.data.ILiveData;
import dk.gormkrings.engine.schedule.IScheduleFactory;
import dk.gormkrings.factory.IDateFactory;
import dk.gormkrings.factory.IResultFactory;
import dk.gormkrings.factory.ISnapshotFactory;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.phase.callBased.SimulationCallPhase;
import dk.gormkrings.returns.IReturner;
import dk.gormkrings.simulation.ReturnStep;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.ITaxExemption;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ScheduleEngineTradingDayConsistencyTest {

    @Test
    void dailyMode_appliesReturnsOnlyOnTradingDays() {
        IDateFactory dateFactory = new dk.gormkrings.simulation.factory.DefaultDateFactory();
        IScheduleFactory scheduleFactory = new DefaultScheduleFactory(dateFactory);

        IResultFactory resultFactory = mock(IResultFactory.class);
        ISnapshotFactory snapshotFactory = mock(ISnapshotFactory.class);

        var result = mock(dk.gormkrings.result.IRunResult.class);
        when(resultFactory.newResult()).thenReturn(result);
        when(snapshotFactory.snapshot(any())).thenReturn(mock(dk.gormkrings.result.ISnapshot.class));

        ScheduleEngine engine = new ScheduleEngine(resultFactory, snapshotFactory, scheduleFactory);

        IDate startDate = dateFactory.dateOf(2025, 1, 1);
        long durationDays = 30;

        var calendar = new WeekdayTradingCalendar();
        CountingReturner returner = new CountingReturner();
        ILiveData liveData = liveDataWithSessionClockAndCapital(100.0);

        ISpecification spec = mock(ISpecification.class);
        when(spec.getLiveData()).thenReturn(liveData);
        when(spec.getReturner()).thenReturn(returner);

        IPhase phase = new MinimalCallPhase(spec, startDate, durationDays, ReturnStep.DAILY, calendar);

        engine.init(List.of(phase));
        engine.simulatePhases(new LinkedList<>(List.of(phase)));

        long expectedTradingDays = countTradingDays(startDate, durationDays, calendar);
        assertEquals(expectedTradingDays, returner.calls, "daily mode should apply returns only on trading days");
    }

    @Test
    void monthlyMode_doesNotApplyDailyReturns() {
        IDateFactory dateFactory = new dk.gormkrings.simulation.factory.DefaultDateFactory();
        IScheduleFactory scheduleFactory = new DefaultScheduleFactory(dateFactory);

        IResultFactory resultFactory = mock(IResultFactory.class);
        ISnapshotFactory snapshotFactory = mock(ISnapshotFactory.class);

        var result = mock(dk.gormkrings.result.IRunResult.class);
        when(resultFactory.newResult()).thenReturn(result);
        when(snapshotFactory.snapshot(any())).thenReturn(mock(dk.gormkrings.result.ISnapshot.class));

        ScheduleEngine engine = new ScheduleEngine(resultFactory, snapshotFactory, scheduleFactory);

        // 10 days from the 1st should not include calendar month-end.
        IDate startDate = dateFactory.dateOf(2025, 1, 1);
        long durationDays = 10;

        var calendar = new WeekdayTradingCalendar();
        CountingReturner returner = new CountingReturner();
        ILiveData liveData = liveDataWithSessionClockAndCapital(100.0);

        ISpecification spec = mock(ISpecification.class);
        when(spec.getLiveData()).thenReturn(liveData);
        when(spec.getReturner()).thenReturn(returner);

        IPhase phase = new MinimalCallPhase(spec, startDate, durationDays, ReturnStep.MONTHLY, calendar);

        engine.init(List.of(phase));
        engine.simulatePhases(new LinkedList<>(List.of(phase)));

        assertEquals(0, returner.calls, "monthly mode should not apply returns on day events");
    }

    private static long countTradingDays(IDate startDate, long durationDays, WeekdayTradingCalendar calendar) {
        long count = 0;
        // Schedule engine increments time before calling onDayEnd.
        for (long sessionDay = 1; sessionDay <= durationDays; sessionDay++) {
            IDate d = startDate.plusDays(sessionDay);
            if (calendar.isTradingDay(d)) count++;
        }
        return count;
    }

    private static ILiveData liveDataWithSessionClockAndCapital(double capital) {
        ILiveData liveData = mock(ILiveData.class);
        AtomicLong session = new AtomicLong(0);

        when(liveData.getCapital()).thenReturn(capital);
        when(liveData.getSessionDuration()).thenAnswer(inv -> session.get());

        doAnswer(inv -> {
            session.incrementAndGet();
            return null;
        }).when(liveData).incrementTime();

        doAnswer(inv -> {
            session.addAndGet(inv.getArgument(0));
            return null;
        }).when(liveData).incrementTime(anyLong());

        doAnswer(inv -> {
            session.set(0);
            return null;
        }).when(liveData).resetSession();

        // No-ops for return application bookkeeping.
        doNothing().when(liveData).setCurrentReturn(anyDouble());
        doNothing().when(liveData).addToReturned(anyDouble());
        doNothing().when(liveData).addToCapital(anyDouble());

        return liveData;
    }

    private static final class CountingReturner implements IReturner {
        long calls;

        @Override
        public double calculateReturn(double amount) {
            calls++;
            return 1.0;
        }

        @Override
        public IReturner copy() {
            return new CountingReturner();
        }
    }

    private static final class MinimalCallPhase extends SimulationCallPhase {
        MinimalCallPhase(ISpecification specification, IDate startDate, long duration, ReturnStep returnStep, WeekdayTradingCalendar calendar) {
            super(specification, startDate, List.<ITaxExemption>of(), duration, "minimal", returnStep, calendar);
        }

        @Override
        public IPhase copy(ISpecification specificationCopy) {
            return new MinimalCallPhase(specificationCopy, getStartDate(), getDuration(), getReturnStep(), (WeekdayTradingCalendar) getTradingCalendar());
        }
    }
}
