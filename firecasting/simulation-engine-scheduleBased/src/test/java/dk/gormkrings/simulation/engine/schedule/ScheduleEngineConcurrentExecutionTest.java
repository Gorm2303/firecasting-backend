package dk.gormkrings.simulation.engine.schedule;

import dk.gormkrings.data.ILiveData;
import dk.gormkrings.engine.schedule.ISchedule;
import dk.gormkrings.engine.schedule.IScheduleFactory;
import dk.gormkrings.factory.IResultFactory;
import dk.gormkrings.factory.ISnapshotFactory;
import dk.gormkrings.phase.ICallPhase;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.result.IRunResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ScheduleEngineConcurrentExecutionTest {

    @Test
    public void testConcurrentSimulatePhasesStateIsolation() throws Exception {
        // Create an executor with two threads.
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Simulation Task 1: Each simulation creates its own mocks and then verifies
        // that only its own mocks were used.
        Callable<IRunResult> simulationTask1 = () -> {
            // Create mocks for simulation 1.
            IResultFactory resultFactory1 = mock(IResultFactory.class);
            // Snapshot factory is not used in our verifications below.
            ISnapshotFactory snapshotFactory1 = mock(ISnapshotFactory.class);
            IScheduleFactory scheduleFactory1 = mock(IScheduleFactory.class);
            ICallPhase phase1 = mock(ICallPhase.class);
            ILiveData liveData1 = mock(ILiveData.class);
            when(phase1.getLiveData()).thenReturn(liveData1);

            // Use an empty schedule for simplicity.
            ISchedule schedule1 = mock(ISchedule.class);
            when(schedule1.getEvents()).thenReturn(Collections.emptyList());
            when(scheduleFactory1.getSchedule()).thenReturn(schedule1);

            // Create a mock result for simulation 1.
            IRunResult result1 = mock(IRunResult.class);
            when(resultFactory1.newResult()).thenReturn(result1);

            // Prepare a LinkedList with a single phase.
            List<IPhase> phaseList1 = new LinkedList<>();
            phaseList1.add(phase1);

            // Create the ScheduleEngine instance for simulation 1.
            ScheduleEngine engine1 = new ScheduleEngine(resultFactory1, snapshotFactory1, scheduleFactory1);
            IRunResult res1 = engine1.simulatePhases(phaseList1);

            // Verify that only simulation 1's mocks were used.
            verify(resultFactory1, times(1)).newResult();
            verify(scheduleFactory1, times(1)).getSchedule();
            // No event-specific methods should be invoked on phase1 since schedule is empty.
            verify(phase1, never()).onMonthStart();
            verify(phase1, never()).onMonthEnd();
            verify(phase1, never()).onYearStart();
            verify(phase1, never()).onYearEnd();
            verify(phase1, never()).onPhaseStart();
            verify(phase1, never()).onPhaseEnd();

            return res1;
        };

        // Simulation Task 2: A separate simulation with its own mocks.
        Callable<IRunResult> simulationTask2 = () -> {
            // Create mocks for simulation 2.
            IResultFactory resultFactory2 = mock(IResultFactory.class);
            ISnapshotFactory snapshotFactory2 = mock(ISnapshotFactory.class);
            IScheduleFactory scheduleFactory2 = mock(IScheduleFactory.class);
            ICallPhase phase2 = mock(ICallPhase.class);
            ILiveData liveData2 = mock(ILiveData.class);
            when(phase2.getLiveData()).thenReturn(liveData2);

            // Use an empty schedule for simulation 2.
            ISchedule schedule2 = mock(ISchedule.class);
            when(schedule2.getEvents()).thenReturn(Collections.emptyList());
            when(scheduleFactory2.getSchedule()).thenReturn(schedule2);

            // Create a mock result for simulation 2.
            IRunResult result2 = mock(IRunResult.class);
            when(resultFactory2.newResult()).thenReturn(result2);

            // Prepare a LinkedList with a single phase.
            List<IPhase> phaseList2 = new LinkedList<>();
            phaseList2.add(phase2);

            // Create the ScheduleEngine instance for simulation 2.
            ScheduleEngine engine2 = new ScheduleEngine(resultFactory2, snapshotFactory2, scheduleFactory2);
            IRunResult res2 = engine2.simulatePhases(phaseList2);

            // Verify that only simulation 2's mocks were used.
            verify(resultFactory2, times(1)).newResult();
            verify(scheduleFactory2, times(1)).getSchedule();
            verify(phase2, never()).onMonthStart();
            verify(phase2, never()).onMonthEnd();
            verify(phase2, never()).onYearStart();
            verify(phase2, never()).onYearEnd();
            verify(phase2, never()).onPhaseStart();
            verify(phase2, never()).onPhaseEnd();

            return res2;
        };

        // Submit both simulation tasks concurrently.
        Future<IRunResult> future1 = executor.submit(simulationTask1);
        Future<IRunResult> future2 = executor.submit(simulationTask2);

        // Wait for the tasks to complete.
        IRunResult result1 = future1.get(5, TimeUnit.SECONDS);
        IRunResult result2 = future2.get(5, TimeUnit.SECONDS);

        // Assert that both simulation results are non-null and different.
        assertNotNull(result1, "Simulation 1 should produce a non-null result");
        assertNotNull(result2, "Simulation 2 should produce a non-null result");
        assertNotEquals(result1, result2, "Simulation 1 and 2 should produce different results");

        executor.shutdown();
    }
}
