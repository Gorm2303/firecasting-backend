package dk.gormkrings.simulation.engine.schedule;

import dk.gormkrings.data.IDate;
import dk.gormkrings.data.ILiveData;
import dk.gormkrings.engine.schedule.ISchedule;
import dk.gormkrings.engine.schedule.IScheduleEvent;
import dk.gormkrings.engine.schedule.IScheduleFactory;
import dk.gormkrings.event.EventType;
import dk.gormkrings.factory.IDateFactory;
import dk.gormkrings.factory.IResultFactory;
import dk.gormkrings.factory.ISnapshotFactory;
import dk.gormkrings.phase.ICallPhase;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.result.IResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ScheduleEngineSimulatePhasesTest {

    @Mock
    protected IDateFactory dateFactory;

    @Mock
    protected ICallPhase phase;

    @Mock
    protected IDate startDate;

    @Mock
    protected IResultFactory resultFactory;

    @Mock
    protected ISnapshotFactory snapshotFactory;

    @Mock
    protected ILiveData liveData;

    // This is the concrete implementation of IScheduleFactory used by the engine.
    protected IScheduleFactory scheduleFactory;

    @BeforeEach
    public void setUp() {
        // Initialize the scheduleFactory with the dateFactory.
        scheduleFactory = new DefaultScheduleFactory(dateFactory);

        // Setup lenient stubbing for phase and startDate.
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

        // Additional stubbing for phase's live data.
        lenient().when(phase.getLiveData()).thenReturn(liveData);
    }

    @Test
    public void testSimulatePhasesSinglePhaseAllEvents() {
        ISchedule schedule = mock(ISchedule.class);

        IScheduleEvent eventMonthStart = mock(IScheduleEvent.class);
        IScheduleEvent eventMonthEnd = mock(IScheduleEvent.class);
        IScheduleEvent eventYearStart = mock(IScheduleEvent.class);
        IScheduleEvent eventYearEnd = mock(IScheduleEvent.class);
        IScheduleEvent eventPhaseStart = mock(IScheduleEvent.class);
        IScheduleEvent eventPhaseEnd = mock(IScheduleEvent.class);

        when(eventMonthStart.getType()).thenReturn(EventType.MONTH_START);
        when(eventMonthEnd.getType()).thenReturn(EventType.MONTH_END);
        when(eventYearStart.getType()).thenReturn(EventType.YEAR_START);
        when(eventYearEnd.getType()).thenReturn(EventType.YEAR_END);
        when(eventPhaseStart.getType()).thenReturn(EventType.PHASE_START);
        when(eventPhaseEnd.getType()).thenReturn(EventType.PHASE_END);

        int dummyEpoch = 1000;
        when(eventMonthEnd.getEpoch()).thenReturn(dummyEpoch);

        List<IScheduleEvent> events = Arrays.asList(
                eventMonthStart,
                eventMonthEnd,
                eventYearStart,
                eventYearEnd,
                eventPhaseStart,
                eventPhaseEnd
        );
        when(schedule.getEvents()).thenReturn(events);

        IScheduleFactory spyScheduleFactory = spy(scheduleFactory);
        doReturn(schedule).when(spyScheduleFactory).getSchedule();

        when(liveData.getSessionDuration()).thenReturn(10L);
        long expectedTimeIncrement = dummyEpoch - (10);

        IResult result = mock(IResult.class);
        when(resultFactory.newResult()).thenReturn(result);

        List<IPhase> phaseList = new LinkedList<>();
        phaseList.add(phase);

        ScheduleEngine engine = new ScheduleEngine(resultFactory, snapshotFactory, spyScheduleFactory);

        IResult simulationResult = engine.simulatePhases(phaseList);

        verify(resultFactory, times(1)).newResult();
        verify(spyScheduleFactory, times(1)).getSchedule();

        verify(phase, times(1)).onMonthStart();
        verify(phase, times(1)).onMonthEnd();
        verify(phase, times(1)).onYearStart();
        verify(phase, times(1)).onYearEnd();
        verify(phase, times(1)).onPhaseStart();
        verify(phase, times(1)).onPhaseEnd();

        verify(liveData, times(1)).incrementTime(eq(expectedTimeIncrement));

        assertSame(result, simulationResult, "simulatePhases should return the result from resultFactory");
    }

    @Test
    public void testPhaseMethodInvocationOrder() {
        // Create mocks for factories.
        IResultFactory resultFactory = mock(IResultFactory.class);
        ISnapshotFactory snapshotFactory = mock(ISnapshotFactory.class);
        IScheduleFactory scheduleFactory = mock(IScheduleFactory.class);

        // Create a mock result and stub resultFactory.
        IResult result = mock(IResult.class);
        when(resultFactory.newResult()).thenReturn(result);

        // Create a mock schedule.
        ISchedule schedule = mock(ISchedule.class);

        // Create mocks for three schedule events.
        IScheduleEvent eventMonthStart = mock(IScheduleEvent.class);
        IScheduleEvent eventPhaseStart = mock(IScheduleEvent.class);
        IScheduleEvent eventPhaseEnd = mock(IScheduleEvent.class);

        // Stub each event to return its type using doReturn.
        doReturn(EventType.MONTH_START).when(eventMonthStart).getType();
        doReturn(EventType.PHASE_START).when(eventPhaseStart).getType();
        doReturn(EventType.PHASE_END).when(eventPhaseEnd).getType();

        // Setup the schedule to return events in the order: MONTH_START, PHASE_START, PHASE_END.
        List<IScheduleEvent> events = Arrays.asList(eventMonthStart, eventPhaseStart, eventPhaseEnd);
        when(schedule.getEvents()).thenReturn(events);
        when(scheduleFactory.getSchedule()).thenReturn(schedule);

        // Create a mock phase and its associated live data.
        ICallPhase phase = mock(ICallPhase.class);
        ILiveData liveData = mock(ILiveData.class);
        when(phase.getLiveData()).thenReturn(liveData);

        // Prepare a LinkedList with one phase.
        List<IPhase> phaseList = new LinkedList<>();
        phaseList.add(phase);

        // Create the ScheduleEngine.
        ScheduleEngine engine = new ScheduleEngine(resultFactory, snapshotFactory, scheduleFactory);

        // Execute simulatePhases.
        engine.simulatePhases(phaseList);

        // Use InOrder to verify that the phase methods are called in the specific order.
        InOrder inOrder = inOrder(phase);

        // Verify that onMonthStart() is called first (for MONTH_START event),
        // then onPhaseStart() (for PHASE_START event), and finally onPhaseEnd() (for PHASE_END event).
        inOrder.verify(phase).onMonthStart();
        inOrder.verify(phase).onPhaseStart();
        inOrder.verify(phase).onPhaseEnd();
    }

    @Test
    public void testStaticScheduleFactoryOverwritten() throws Exception {
        // Create mocks for non-static dependencies.
        IResultFactory resultFactory = mock(IResultFactory.class);
        ISnapshotFactory snapshotFactory = mock(ISnapshotFactory.class);

        // Create two different mocks for scheduleFactory.
        IScheduleFactory scheduleFactory1 = mock(IScheduleFactory.class);
        IScheduleFactory scheduleFactory2 = mock(IScheduleFactory.class);

        // Create first instance of ScheduleEngine using scheduleFactory1.
        ScheduleEngine engine1 = new ScheduleEngine(resultFactory, snapshotFactory, scheduleFactory1);

        // Use reflection to access the private static field "scheduleFactory".
        Field staticField = ScheduleEngine.class.getDeclaredField("scheduleFactory");
        staticField.setAccessible(true);

        // Assert that the static scheduleFactory is scheduleFactory1.
        assertSame(scheduleFactory1, staticField.get(null),
                "The static scheduleFactory should be set to scheduleFactory1");

        // Create second instance of ScheduleEngine using scheduleFactory2.
        ScheduleEngine engine2 = new ScheduleEngine(resultFactory, snapshotFactory, scheduleFactory2);

        // Assert that the static scheduleFactory has been overwritten with scheduleFactory2.
        assertSame(scheduleFactory2, staticField.get(null),
                "The static scheduleFactory should now be overwritten with scheduleFactory2");
    }
}
