package dk.gormkrings.simulation.engine.schedule;

import dk.gormkrings.data.IDate;
import dk.gormkrings.engine.schedule.ISchedule;
import dk.gormkrings.engine.schedule.IScheduleEvent;
import dk.gormkrings.event.EventType;
import dk.gormkrings.factory.IDateFactory;
import dk.gormkrings.phase.ICallPhase;
import dk.gormkrings.phase.IPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DefaultScheduleFactoryBoundaryTest {

    @Mock
    protected IDateFactory dateFactory;

    @Mock
    protected ICallPhase phase;

    @Mock
    protected IDate startDate;

    protected DefaultScheduleFactory scheduleFactory;

    @BeforeEach
    public void setUp() {
        scheduleFactory = new DefaultScheduleFactory(dateFactory);
        lenient().when(phase.getStartDate()).thenReturn(startDate);
        lenient().when(phase.getDuration()).thenReturn(10L);

        // Stubbings are lenient because they might not be used in every test.
        lenient().when(startDate.getEpochDay()).thenReturn(1);
        lenient().when(startDate.computeNextWeekStart()).thenReturn(2);
        lenient().when(startDate.computeWeekEnd()).thenReturn(3);
        lenient().when(startDate.computeNextMonthStart()).thenReturn(4);
        lenient().when(startDate.computeNextMonthEnd()).thenReturn(5);
        lenient().when(startDate.computeNextYearStart()).thenReturn(6);
        lenient().when(startDate.computeNextYearEnd()).thenReturn(7);

        lenient().when(dateFactory.fromEpochDay(anyInt())).thenReturn(startDate);
    }

    @Test
    public void testBoundaryEventChecks() {
        when(phase.getDuration()).thenReturn(7L);
        when(phase.supportsEvent(EventType.DAY_START)).thenReturn(true);
        when(phase.supportsEvent(EventType.DAY_END)).thenReturn(true);
        when(phase.supportsEvent(EventType.WEEK_START)).thenReturn(true);
        when(phase.supportsEvent(EventType.WEEK_END)).thenReturn(true);
        when(phase.supportsEvent(EventType.MONTH_START)).thenReturn(true);
        when(phase.supportsEvent(EventType.MONTH_END)).thenReturn(true);
        when(phase.supportsEvent(EventType.YEAR_START)).thenReturn(true);
        when(phase.supportsEvent(EventType.YEAR_END)).thenReturn(true);

        List<IPhase> phases = Collections.singletonList(phase);
        ISchedule schedule = scheduleFactory.build(phases);
        List<IScheduleEvent> events = ((Schedule) schedule).getEvents();

        assertTrue(events.stream().anyMatch(e ->
                        e.getType() == EventType.WEEK_START && e.getEpoch() == 2),
                "WEEK_START event should be added at epoch 2");

        assertTrue(events.stream().anyMatch(e ->
                        e.getType() == EventType.WEEK_END && e.getEpoch() == 3),
                "WEEK_END event should be added at epoch 3");

        assertTrue(events.stream().anyMatch(e ->
                        e.getType() == EventType.MONTH_START && e.getEpoch() == 4),
                "MONTH_START event should be added at epoch 4");

        assertTrue(events.stream().anyMatch(e ->
                        e.getType() == EventType.MONTH_END && e.getEpoch() == 5),
                "MONTH_END event should be added at epoch 5");

        assertTrue(events.stream().anyMatch(e ->
                        e.getType() == EventType.YEAR_START && e.getEpoch() == 6),
                "YEAR_START event should be added at epoch 6");

        assertTrue(events.stream().anyMatch(e ->
                        e.getType() == EventType.YEAR_END && e.getEpoch() == 7),
                "YEAR_END event should be added at epoch 7");
    }

    @Test
    public void testWeekStartBoundaryUpdatesNextBoundary() {
        when(phase.getDuration()).thenReturn(5L);
        when(phase.supportsEvent(EventType.WEEK_START)).thenReturn(true);
        when(phase.supportsEvent(EventType.DAY_START)).thenReturn(false);
        when(phase.supportsEvent(EventType.DAY_END)).thenReturn(false);
        when(phase.supportsEvent(EventType.WEEK_END)).thenReturn(false);
        when(phase.supportsEvent(EventType.MONTH_START)).thenReturn(false);
        when(phase.supportsEvent(EventType.MONTH_END)).thenReturn(false);
        when(phase.supportsEvent(EventType.YEAR_START)).thenReturn(false);
        when(phase.supportsEvent(EventType.YEAR_END)).thenReturn(false);

        IDate newDateMock = mock(IDate.class);
        when(newDateMock.computeNextWeekStart()).thenReturn(10);
        when(dateFactory.fromEpochDay(2)).thenReturn(newDateMock);

        List<IPhase> phases = Collections.singletonList(phase);
        scheduleFactory.build(phases);

        verify(dateFactory).fromEpochDay(2);
        verify(newDateMock).computeNextWeekStart();
    }

    @Test
    public void testNegativeDurationOrInvalidInput() {
        when(phase.getDuration()).thenReturn(-1L);
        when(startDate.getEpochDay()).thenReturn(50);
        when(phase.supportsEvent(any())).thenReturn(false);

        when(startDate.computeNextWeekStart()).thenReturn(60);
        when(startDate.computeWeekEnd()).thenReturn(61);
        when(startDate.computeNextMonthStart()).thenReturn(62);
        when(startDate.computeNextMonthEnd()).thenReturn(63);
        when(startDate.computeNextYearStart()).thenReturn(64);
        when(startDate.computeNextYearEnd()).thenReturn(65);

        List<IPhase> phases = Collections.singletonList(phase);
        ISchedule schedule = scheduleFactory.build(phases);
        List<IScheduleEvent> events = ((Schedule) schedule).getEvents();

        assertEquals(2, events.size(), "There should be exactly 2 events for a phase with negative duration");

        IScheduleEvent phaseStart = events.get(0);
        IScheduleEvent phaseEnd = events.get(1);

        assertEquals(EventType.PHASE_START, phaseStart.getType(), "First event should be PHASE_START");
        assertEquals(50, phaseStart.getEpoch(), "PHASE_START should be at epoch 50");

        assertEquals(EventType.PHASE_END, phaseEnd.getType(), "Second event should be PHASE_END");
        assertEquals(49, phaseEnd.getEpoch(), "PHASE_END should be at epoch 49 (50 - 1)");
    }

    @Test
    public void testBoundaryValueAnomalies() {
        when(startDate.getEpochDay()).thenReturn(100);
        when(phase.getDuration()).thenReturn(10L);

        when(startDate.computeNextWeekStart()).thenReturn(100);
        when(startDate.computeWeekEnd()).thenReturn(100);
        when(startDate.computeNextMonthStart()).thenReturn(105);
        when(startDate.computeNextMonthEnd()).thenReturn(105);
        when(startDate.computeNextYearStart()).thenReturn(500);
        when(startDate.computeNextYearEnd()).thenReturn(500);

        when(phase.supportsEvent(EventType.DAY_START)).thenReturn(false);
        when(phase.supportsEvent(EventType.DAY_END)).thenReturn(false);
        when(phase.supportsEvent(EventType.WEEK_START)).thenReturn(true);
        when(phase.supportsEvent(EventType.WEEK_END)).thenReturn(true);
        when(phase.supportsEvent(EventType.MONTH_START)).thenReturn(true);
        when(phase.supportsEvent(EventType.MONTH_END)).thenReturn(true);
        when(phase.supportsEvent(EventType.YEAR_START)).thenReturn(true);
        when(phase.supportsEvent(EventType.YEAR_END)).thenReturn(true);

        List<IPhase> phases = Collections.singletonList(phase);
        ISchedule schedule = scheduleFactory.build(phases);
        List<IScheduleEvent> events = ((Schedule) schedule).getEvents();

        assertEquals(6, events.size(), "Expected 6 events with anomalous boundaries");

        IScheduleEvent phaseStart = events.get(0);
        assertEquals(EventType.PHASE_START, phaseStart.getType(), "First event should be PHASE_START");
        assertEquals(100, phaseStart.getEpoch(), "PHASE_START should be at epoch 100");

        IScheduleEvent weekStart = events.get(1);
        IScheduleEvent weekEnd = events.get(2);
        assertEquals(EventType.WEEK_START, weekStart.getType(), "Expected WEEK_START event");
        assertEquals(100, weekStart.getEpoch(), "WEEK_START should be at epoch 100");
        assertEquals(EventType.WEEK_END, weekEnd.getType(), "Expected WEEK_END event");
        assertEquals(100, weekEnd.getEpoch(), "WEEK_END should be at epoch 100");

        IScheduleEvent monthStart = events.get(3);
        IScheduleEvent monthEnd = events.get(4);
        assertEquals(EventType.MONTH_START, monthStart.getType(), "Expected MONTH_START event");
        assertEquals(105, monthStart.getEpoch(), "MONTH_START should be at epoch 105");
        assertEquals(EventType.MONTH_END, monthEnd.getType(), "Expected MONTH_END event");
        assertEquals(105, monthEnd.getEpoch(), "MONTH_END should be at epoch 105");

        IScheduleEvent phaseEnd = events.get(5);
        assertEquals(EventType.PHASE_END, phaseEnd.getType(), "Expected PHASE_END event");
        assertEquals(109, phaseEnd.getEpoch(), "PHASE_END should be at epoch 109");
    }

    @Test
    public void testRepeatedBoundaryTriggersOnConsecutiveDays() {
        when(startDate.getEpochDay()).thenReturn(100);
        when(phase.getDuration()).thenReturn(3L);

        when(phase.supportsEvent(EventType.YEAR_END)).thenReturn(true);
        when(phase.supportsEvent(EventType.DAY_START)).thenReturn(false);
        when(phase.supportsEvent(EventType.DAY_END)).thenReturn(false);
        when(phase.supportsEvent(EventType.WEEK_START)).thenReturn(false);
        when(phase.supportsEvent(EventType.WEEK_END)).thenReturn(false);
        when(phase.supportsEvent(EventType.MONTH_START)).thenReturn(false);
        when(phase.supportsEvent(EventType.MONTH_END)).thenReturn(false);
        when(phase.supportsEvent(EventType.YEAR_START)).thenReturn(false);

        when(startDate.computeNextYearEnd()).thenReturn(100);

        IDate dateFor100 = mock(IDate.class);
        when(dateFactory.fromEpochDay(100)).thenReturn(dateFor100);
        when(dateFor100.computeNextYearEnd()).thenReturn(101);

        IDate dateFor101 = mock(IDate.class);
        when(dateFactory.fromEpochDay(101)).thenReturn(dateFor101);
        when(dateFor101.computeNextYearEnd()).thenReturn(102);

        IDate dateFor102 = mock(IDate.class);
        when(dateFactory.fromEpochDay(102)).thenReturn(dateFor102);
        when(dateFor102.computeNextYearEnd()).thenReturn(103);

        lenient().when(startDate.computeNextWeekStart()).thenReturn(200);
        lenient().when(startDate.computeWeekEnd()).thenReturn(201);
        lenient().when(startDate.computeNextMonthStart()).thenReturn(202);
        lenient().when(startDate.computeNextMonthEnd()).thenReturn(203);
        lenient().when(startDate.computeNextYearStart()).thenReturn(300);

        List<IPhase> phases = Collections.singletonList(phase);
        ISchedule schedule = scheduleFactory.build(phases);
        List<IScheduleEvent> events = ((Schedule) schedule).getEvents();

        assertEquals(5, events.size(), "Expected 5 events (PHASE_START, 3 YEAR_END, PHASE_END)");

        IScheduleEvent phaseStart = events.get(0);
        assertEquals(EventType.PHASE_START, phaseStart.getType(), "First event should be PHASE_START");
        assertEquals(100, phaseStart.getEpoch(), "PHASE_START should be at epoch 100");

        // Verify YEAR_END events.
        IScheduleEvent yearEnd1 = events.get(1);
        IScheduleEvent yearEnd2 = events.get(2);
        IScheduleEvent yearEnd3 = events.get(3);

        assertEquals(EventType.YEAR_END, yearEnd1.getType(), "First YEAR_END event expected");
        assertEquals(100, yearEnd1.getEpoch(), "First YEAR_END event should be at epoch 100");

        assertEquals(EventType.YEAR_END, yearEnd2.getType(), "Second YEAR_END event expected");
        assertEquals(101, yearEnd2.getEpoch(), "Second YEAR_END event should be at epoch 101");

        assertEquals(EventType.YEAR_END, yearEnd3.getType(), "Third YEAR_END event expected");
        assertEquals(102, yearEnd3.getEpoch(), "Third YEAR_END event should be at epoch 102");

        IScheduleEvent phaseEnd = events.get(4);
        assertEquals(EventType.PHASE_END, phaseEnd.getType(), "Expected PHASE_END event");
        assertEquals(102, phaseEnd.getEpoch(), "PHASE_END should be at epoch 102");
    }
}
