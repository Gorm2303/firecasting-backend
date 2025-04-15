package dk.gormkrings.simulation.engine.schedule;

import dk.gormkrings.data.IDate;
import dk.gormkrings.data.ILiveData;
import dk.gormkrings.engine.schedule.ISchedule;
import dk.gormkrings.engine.schedule.IScheduleEvent;
import dk.gormkrings.event.EventType;
import dk.gormkrings.factory.IDateFactory;
import dk.gormkrings.factory.IResultFactory;
import dk.gormkrings.factory.ISnapshotFactory;
import dk.gormkrings.phase.ICallPhase;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.result.IRunResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ScheduleEngineMultiplePhasesTest {

    @Mock
    protected IDateFactory dateFactory;

    @Mock
    protected ICallPhase phase1;

    @Mock
    protected ICallPhase phase2;

    @Mock
    protected IDate startDate;

    @Mock
    protected IResultFactory resultFactory;

    @Mock
    protected ISnapshotFactory snapshotFactory;

    @Mock
    protected ILiveData liveData1;

    @Mock
    protected ILiveData liveData2;

    protected DefaultScheduleFactory scheduleFactory;

    @BeforeEach
    public void setUp() {
        scheduleFactory = new DefaultScheduleFactory(dateFactory);

        lenient().when(phase1.getStartDate()).thenReturn(startDate);
        lenient().when(phase1.getDuration()).thenReturn(10L);
        lenient().when(startDate.getEpochDay()).thenReturn(1);
        lenient().when(dateFactory.fromEpochDay(any(Integer.class))).thenReturn(startDate);

        when(phase1.getLiveData()).thenReturn(liveData1);
        when(phase2.getLiveData()).thenReturn(liveData2);
    }

    @Test
    public void testSimulatePhasesMultiplePhasesTransition() {
        ISchedule schedule = mock(ISchedule.class);

        IScheduleEvent eventPhaseStart = mock(IScheduleEvent.class);
        IScheduleEvent eventPhaseEnd = mock(IScheduleEvent.class);
        IScheduleEvent eventMonthStart = mock(IScheduleEvent.class);

        when(eventPhaseStart.getType()).thenReturn(EventType.PHASE_START);
        when(eventPhaseEnd.getType()).thenReturn(EventType.PHASE_END);
        when(eventMonthStart.getType()).thenReturn(EventType.MONTH_START);

        List<IScheduleEvent> events = Arrays.asList(
                eventPhaseStart,
                eventPhaseEnd,
                eventMonthStart
        );
        when(schedule.getEvents()).thenReturn(events);

        DefaultScheduleFactory spyScheduleFactory = spy(scheduleFactory);
        doReturn(schedule).when(spyScheduleFactory).getSchedule();

        IRunResult result = mock(IRunResult.class);
        when(resultFactory.newResult()).thenReturn(result);

        List<IPhase> phaseList = new LinkedList<>();
        phaseList.add(phase1);
        phaseList.add(phase2);

        ScheduleEngine engine = new ScheduleEngine(resultFactory, snapshotFactory, spyScheduleFactory);

        IRunResult simulationResult = engine.simulatePhases(phaseList);

        verify(resultFactory, times(1)).newResult();
        verify(spyScheduleFactory, times(1)).getSchedule();
        verify(phase1, times(1)).onPhaseStart();
        verify(phase1, times(1)).onPhaseEnd();
        verify(liveData2, times(1)).resetSession();
        verify(phase2, times(1)).onMonthStart();

        assertSame(result, simulationResult, "simulatePhases should return the result from resultFactory");
    }
}
