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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DefaultScheduleFactoryEventGenerationTest {

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
        lenient().when(startDate.computeMonthEnd()).thenReturn(5);
        lenient().when(startDate.computeNextYearStart()).thenReturn(6);
        lenient().when(startDate.computeYearEnd()).thenReturn(7);

        lenient().when(dateFactory.fromEpochDay(anyInt())).thenReturn(startDate);
    }

    @Test
    public void testEventGenerationPerPhase() {
        when(phase.supportsEvent(EventType.DAY_START)).thenReturn(true);
        when(phase.supportsEvent(EventType.DAY_END)).thenReturn(true);
        when(phase.supportsEvent(EventType.WEEK_START)).thenReturn(false);
        when(phase.supportsEvent(EventType.WEEK_END)).thenReturn(false);
        when(phase.supportsEvent(EventType.MONTH_START)).thenReturn(false);
        when(phase.supportsEvent(EventType.MONTH_END)).thenReturn(false);
        when(phase.supportsEvent(EventType.YEAR_START)).thenReturn(false);
        when(phase.supportsEvent(EventType.YEAR_END)).thenReturn(false);

        List<IPhase> phases = Collections.singletonList(phase);
        ISchedule schedule = scheduleFactory.build(phases);

        List<IScheduleEvent> events = ((Schedule) schedule).getEvents();

        assertEquals(22, events.size(), "Expected 22 events generated for a 10-day phase with day events enabled");

        IScheduleEvent firstDayStart = events.get(1);
        IScheduleEvent firstDayEnd = events.get(2);
        assertEquals(EventType.DAY_START, firstDayStart.getType(), "First event should be DAY_START");
        assertEquals(1, firstDayStart.getEpoch(), "DAY_START event should be at epoch 1");
        assertEquals(EventType.DAY_END, firstDayEnd.getType(), "Second event should be DAY_END");
        assertEquals(1, firstDayEnd.getEpoch(), "DAY_END event should be at epoch 1");

        IScheduleEvent phaseStartEvent = events.getFirst();
        IScheduleEvent phaseEndEvent = events.getLast();
        assertEquals(EventType.PHASE_START, phaseStartEvent.getType(), "Expected PHASE_START event");
        assertEquals(1, phaseStartEvent.getEpoch(), "PHASE_START event should be at epoch 1");
        assertEquals(EventType.PHASE_END, phaseEndEvent.getType(), "Expected PHASE_END event");
        assertEquals(10, phaseEndEvent.getEpoch(), "PHASE_END event should be at epoch 10");
    }

    @Test
    public void testPhaseStartAndEndEvents() {
        when(phase.supportsEvent(EventType.DAY_START)).thenReturn(false);
        when(phase.supportsEvent(EventType.DAY_END)).thenReturn(false);
        when(phase.supportsEvent(EventType.WEEK_START)).thenReturn(false);
        when(phase.supportsEvent(EventType.WEEK_END)).thenReturn(false);
        when(phase.supportsEvent(EventType.MONTH_START)).thenReturn(false);
        when(phase.supportsEvent(EventType.MONTH_END)).thenReturn(false);
        when(phase.supportsEvent(EventType.YEAR_START)).thenReturn(false);
        when(phase.supportsEvent(EventType.YEAR_END)).thenReturn(false);

        List<IPhase> phases = Collections.singletonList(phase);
        ISchedule schedule = scheduleFactory.build(phases);

        List<IScheduleEvent> events = ((Schedule) schedule).getEvents();

        int expectedStartEpoch = 1;
        int expectedEndEpoch = 10;

        IScheduleEvent phaseStartEvent = events.stream()
                .filter(e -> e.getType() == EventType.PHASE_START)
                .findFirst()
                .orElse(null);
        IScheduleEvent phaseEndEvent = events.stream()
                .filter(e -> e.getType() == EventType.PHASE_END)
                .findFirst()
                .orElse(null);

        assertNotNull(phaseStartEvent, "PHASE_START event should be present in the schedule");
        assertEquals(expectedStartEpoch, phaseStartEvent.getEpoch(),
                "PHASE_START event should be at the start epoch (" + expectedStartEpoch + ")");

        assertNotNull(phaseEndEvent, "PHASE_END event should be present in the schedule");
        assertEquals(expectedEndEpoch, phaseEndEvent.getEpoch(),
                "PHASE_END event should be at the final epoch (" + expectedEndEpoch + ")");
    }

    @Test
    public void testEventSupportSimulation() {
        when(phase.supportsEvent(EventType.DAY_START)).thenReturn(true);
        when(phase.supportsEvent(EventType.DAY_END)).thenReturn(true);
        when(phase.supportsEvent(EventType.WEEK_START)).thenReturn(false);
        when(phase.supportsEvent(EventType.WEEK_END)).thenReturn(false);
        when(phase.supportsEvent(EventType.MONTH_START)).thenReturn(true);
        when(phase.supportsEvent(EventType.MONTH_END)).thenReturn(true);
        when(phase.supportsEvent(EventType.YEAR_START)).thenReturn(false);
        when(phase.supportsEvent(EventType.YEAR_END)).thenReturn(false);

        when(phase.getDuration()).thenReturn(5L);

        lenient().when(startDate.computeNextMonthStart()).thenReturn(2);
        lenient().when(startDate.computeMonthEnd()).thenReturn(4);
        lenient().when(startDate.computeNextWeekStart()).thenReturn(3);
        lenient().when(startDate.computeWeekEnd()).thenReturn(5);
        lenient().when(startDate.computeNextYearStart()).thenReturn(7);
        lenient().when(startDate.computeYearEnd()).thenReturn(8);

        List<IPhase> phases = Collections.singletonList(phase);
        ISchedule schedule = scheduleFactory.build(phases);

        List<IScheduleEvent> events = (schedule).getEvents();

        assertEquals(14, events.size(), "Expected 14 events for the given phase configuration");

        long weekEventsCount = events.stream()
                .filter(e -> e.getType() == EventType.WEEK_START || e.getType() == EventType.WEEK_END)
                .count();
        assertEquals(0, weekEventsCount, "No week events should be added");

        long yearEventsCount = events.stream()
                .filter(e -> e.getType() == EventType.YEAR_START || e.getType() == EventType.YEAR_END)
                .count();
        assertEquals(0, yearEventsCount, "No year events should be added");

        boolean monthStartFound = events.stream()
                .anyMatch(e -> e.getType() == EventType.MONTH_START && e.getEpoch() == 2);
        assertTrue(monthStartFound, "MONTH_START event should be present at epoch 2");

        boolean monthEndFound = events.stream()
                .anyMatch(e -> e.getType() == EventType.MONTH_END && e.getEpoch() == 4);
        assertTrue(monthEndFound, "MONTH_END event should be present at epoch 4");
    }

    @Test
    public void testMultiplePhases() {
        ICallPhase phase2 = mock(ICallPhase.class);
        IDate startDate2 = mock(IDate.class);

        // Configure phase2: start at epoch 20 with a duration of 5 days.
        when(phase2.getStartDate()).thenReturn(startDate2);
        when(phase2.getDuration()).thenReturn(5L);
        when(startDate2.getEpochDay()).thenReturn(20);

        when(phase2.supportsEvent(EventType.DAY_START)).thenReturn(true);
        when(phase2.supportsEvent(EventType.DAY_END)).thenReturn(true);
        when(phase2.supportsEvent(EventType.WEEK_START)).thenReturn(false);
        when(phase2.supportsEvent(EventType.WEEK_END)).thenReturn(false);
        when(phase2.supportsEvent(EventType.MONTH_START)).thenReturn(false);
        when(phase2.supportsEvent(EventType.MONTH_END)).thenReturn(false);
        when(phase2.supportsEvent(EventType.YEAR_START)).thenReturn(false);
        when(phase2.supportsEvent(EventType.YEAR_END)).thenReturn(false);

        // Build the schedule with both phases.
        List<IPhase> phases = java.util.Arrays.asList(phase, phase2);
        ISchedule schedule = scheduleFactory.build(phases);
        List<IScheduleEvent> events = ((Schedule) schedule).getEvents();

        // Verify that phase1 has PHASE_START at epoch 1 and PHASE_END at epoch 10.
        IScheduleEvent phase1Start = events.stream()
                .filter(e -> e.getType() == EventType.PHASE_START && e.getEpoch() == 1)
                .findFirst().orElse(null);
        IScheduleEvent phase1End = events.stream()
                .filter(e -> e.getType() == EventType.PHASE_END && e.getEpoch() == 10)
                .findFirst().orElse(null);
        assertNotNull(phase1Start, "Phase 1 PHASE_START event should be present");
        assertNotNull(phase1End, "Phase 1 PHASE_END event should be present");

        // Verify that phase2 has PHASE_START at epoch 20 and PHASE_END at epoch 24.
        IScheduleEvent phase2Start = events.stream()
                .filter(e -> e.getType() == EventType.PHASE_START && e.getEpoch() == 20)
                .findFirst().orElse(null);
        IScheduleEvent phase2End = events.stream()
                .filter(e -> e.getType() == EventType.PHASE_END && e.getEpoch() == 24)
                .findFirst().orElse(null);
        assertNotNull(phase2Start, "Phase 2 PHASE_START event should be present");
        assertNotNull(phase2End, "Phase 2 PHASE_END event should be present");
    }

    @Test
    public void testEventOrdering() {
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

        for (int i = 1; i < events.size(); i++) {
            assertTrue(events.get(i - 1).getEpoch() <= events.get(i).getEpoch(),
                    "Events should be ordered by epoch");
        }
    }
}
