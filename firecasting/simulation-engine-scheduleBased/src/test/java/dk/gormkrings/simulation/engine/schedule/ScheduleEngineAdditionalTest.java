package dk.gormkrings.simulation.engine.schedule;

import dk.gormkrings.data.ILiveData;
import dk.gormkrings.engine.schedule.ISchedule;
import dk.gormkrings.engine.schedule.IScheduleEvent;
import dk.gormkrings.engine.schedule.IScheduleFactory;
import dk.gormkrings.event.EventType;
import dk.gormkrings.factory.IResultFactory;
import dk.gormkrings.factory.ISnapshotFactory;
import dk.gormkrings.phase.ICallPhase;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.result.IResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ScheduleEngineAdditionalTest {

    @Mock
    private IResultFactory resultFactory;

    @Mock
    private ISnapshotFactory snapshotFactory;

    @Mock
    private IScheduleFactory scheduleFactory;

    @Mock
    private ICallPhase phase;

    @Mock
    private ILiveData liveData;

    @Test
    public void testConstructorInitialization() throws Exception {
        // Create mocks for the dependencies
        IResultFactory resultFactory = mock(IResultFactory.class);
        ISnapshotFactory snapshotFactory = mock(ISnapshotFactory.class);
        IScheduleFactory scheduleFactory = mock(IScheduleFactory.class);

        // Construct the ScheduleEngine instance
        ScheduleEngine engine = new ScheduleEngine(resultFactory, snapshotFactory, scheduleFactory);

        // Verify that the instance fields are correctly assigned
        assertSame(resultFactory, engine.getResultFactory(), "ResultFactory should be assigned correctly");
        assertSame(snapshotFactory, engine.getSnapshotFactory(), "SnapshotFactory should be assigned correctly");

        // Verify that the static scheduleFactory field is correctly assigned using reflection
        Field staticField = ScheduleEngine.class.getDeclaredField("scheduleFactory");
        staticField.setAccessible(true);
        IScheduleFactory actualScheduleFactory = (IScheduleFactory) staticField.get(null); // null for static field
        assertSame(scheduleFactory, actualScheduleFactory, "Static scheduleFactory should be assigned correctly");
    }

    @Test
    public void testInitCallsScheduleFactoryBuildWithPhases() {
        IResultFactory resultFactory = mock(IResultFactory.class);
        ISnapshotFactory snapshotFactory = mock(ISnapshotFactory.class);
        IScheduleFactory scheduleFactory = mock(IScheduleFactory.class);

        IPhase phase1 = mock(IPhase.class);
        IPhase phase2 = mock(IPhase.class);
        List<IPhase> phases = Arrays.asList(phase1, phase2);

        ScheduleEngine engine = new ScheduleEngine(resultFactory, snapshotFactory, scheduleFactory);

        engine.init(phases);

        verify(scheduleFactory).build(phases);
    }

    @Test
    public void testSimulatePhasesNoEvents() {
        IResult result = mock(IResult.class);
        when(resultFactory.newResult()).thenReturn(result);

        ISchedule schedule = mock(ISchedule.class);
        when(schedule.getEvents()).thenReturn(Collections.emptyList());
        when(scheduleFactory.getSchedule()).thenReturn(schedule);

        List<IPhase> phaseList = new LinkedList<>();
        phaseList.add(phase);

        ScheduleEngine engine = new ScheduleEngine(resultFactory, snapshotFactory, scheduleFactory);

        IResult simulationResult = engine.simulatePhases(phaseList);

        verify(resultFactory, times(1)).newResult();
        verify(scheduleFactory, times(1)).getSchedule();

        verify(phase, never()).onMonthStart();
        verify(phase, never()).onMonthEnd();
        verify(phase, never()).onYearStart();
        verify(phase, never()).onYearEnd();
        verify(phase, never()).onPhaseStart();
        verify(phase, never()).onPhaseEnd();

        assertSame(result, simulationResult, "simulatePhases should return the result from resultFactory");
    }

    @Test
    public void testSimulatePhasesEmptyPhaseList() {
        IResultFactory resultFactory = mock(IResultFactory.class);
        ISnapshotFactory snapshotFactory = mock(ISnapshotFactory.class);
        IScheduleFactory scheduleFactory = mock(IScheduleFactory.class);

        ScheduleEngine engine = new ScheduleEngine(resultFactory, snapshotFactory, scheduleFactory);

        List<IPhase> emptyPhaseList = new LinkedList<>();

        assertThrows(NoSuchElementException.class, () -> engine.simulatePhases(emptyPhaseList));
    }

    @Test
    public void testUnexpectedEventTypeThrowsException() {
        // Create mocks for the factories.
        IResultFactory resultFactory = mock(IResultFactory.class);
        ISnapshotFactory snapshotFactory = mock(ISnapshotFactory.class);
        IScheduleFactory scheduleFactory = mock(IScheduleFactory.class);

        // Create a mock result and stub resultFactory.
        IResult result = mock(IResult.class);
        when(resultFactory.newResult()).thenReturn(result);

        // Create a mock schedule that will return one event with an unexpected type.
        ISchedule schedule = mock(ISchedule.class);

        // Create an event whose type is unexpected.
        IScheduleEvent unexpectedEvent = mock(IScheduleEvent.class);
        // Instead of mocking a final enum, simply return null to simulate an unexpected type.
        when(unexpectedEvent.getType()).thenReturn(null);

        // The schedule returns a singleton list with the unexpected event.
        when(schedule.getEvents()).thenReturn(Collections.singletonList(unexpectedEvent));
        when(scheduleFactory.getSchedule()).thenReturn(schedule);

        // Create a mock phase and its live data.
        ICallPhase phase = mock(ICallPhase.class);

        // Prepare a LinkedList with one phase.
        List<IPhase> phaseList = new LinkedList<>();
        phaseList.add(phase);

        // Create the ScheduleEngine instance.
        ScheduleEngine engine = new ScheduleEngine(resultFactory, snapshotFactory, scheduleFactory);

        // Assert that simulatePhases throws a NullPointerException due to the unexpected (null) event type.
        assertThrows(NullPointerException.class, () -> engine.simulatePhases(phaseList),
                "simulatePhases should throw NullPointerException when an event has an unexpected type (null)");
    }
}
