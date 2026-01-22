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
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DefaultScheduleFactoryInitializationTest {

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
    public void testBuildBeforeScheduleCreation() {
        assertNull(scheduleFactory.getSchedule(), "getSchedule() should return null before any build() call");
    }

    @Test
    public void testEmptyPhaseList() {
        List<IPhase> phases = new ArrayList<>();
        ISchedule schedule = scheduleFactory.build(phases);
        assertNotNull(schedule, "A schedule instance should be created even for an empty phase list");
        assertTrue(schedule.getEvents().isEmpty(), "The events list should be empty when no phases are provided");
    }

    @Test
    public void testScheduleImmutability() {
        List<IPhase> phases = new java.util.ArrayList<>(Collections.singletonList(phase));
        ISchedule schedule1 = scheduleFactory.build(phases);
        phases.add(phase);
        ISchedule schedule2 = scheduleFactory.build(phases);
        assertSame(schedule1, schedule2, "Schedule should remain unchanged once built");
    }


    @Test
    public void testInvalidPhaseType() {
        IPhase invalidPhase = mock(IPhase.class);
        List<IPhase> phases = Collections.singletonList(invalidPhase);
        assertThrows(ClassCastException.class, () -> scheduleFactory.build(phases));
    }

    @Test
    public void testZeroDurationPhase() {
        when(startDate.getEpochDay()).thenReturn(0);
        when(phase.getDuration()).thenReturn(0L);

        List<IPhase> phases = Collections.singletonList(phase);
        ISchedule schedule = scheduleFactory.build(phases);
        List<IScheduleEvent> events = ((Schedule) schedule).getEvents();

        assertEquals(2, events.size(), "Expected 4 events for a zero-duration phase");

        IScheduleEvent phaseStart = events.stream()
                .filter(e -> e.getType() == EventType.PHASE_START).findFirst().orElse(null);
        IScheduleEvent phaseEnd = events.stream()
                .filter(e -> e.getType() == EventType.PHASE_END).findFirst().orElse(null);
        assertNotNull(phaseStart, "PHASE_START event should be present");
        assertNotNull(phaseEnd, "PHASE_END event should be present");
        assertEquals(0, phaseStart.getEpoch(), "PHASE_START should be at epoch 1");
        assertEquals(0, phaseEnd.getEpoch(), "PHASE_END should be at epoch 1");
    }
}
