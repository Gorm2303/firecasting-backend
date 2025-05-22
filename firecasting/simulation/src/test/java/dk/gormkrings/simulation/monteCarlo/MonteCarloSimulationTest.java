package dk.gormkrings.simulation.monteCarlo;

import dk.gormkrings.engine.IEngine;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.result.IRunResult;
import dk.gormkrings.specification.ISpecification;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MonteCarloSimulationTest {

    @Test
    public void testValidEngineSelection() {
        IEngine mockEngine = mock(IEngine.class);
        Map<String, IEngine> engines = new HashMap<>();
        engines.put("testEngine", mockEngine);
        MonteCarloSimulation simulation = new MonteCarloSimulation(engines, "testEngine");

        assertNotNull(simulation, "Simulation instance should be created successfully.");
    }

    @Test
    public void testInvalidEngineName() {
        IEngine mockEngine = mock(IEngine.class);
        Map<String, IEngine> engines = new HashMap<>();
        engines.put("someOtherEngine", mockEngine);
        assertThrows(IllegalArgumentException.class, () -> {
            new MonteCarloSimulation(engines, "testEngine");
        });
    }

    @Test
    public void testRunReturnsCorrectNumberOfResults() throws Exception {
        IEngine mockEngine = mock(IEngine.class);
        IRunResult dummyResult = mock(IRunResult.class);
        when(mockEngine.simulatePhases(anyList())).thenReturn(dummyResult);

        IPhase mockPhase1 = mock(IPhase.class);
        IPhase mockPhase2 = mock(IPhase.class);
        ISpecification dummySpecification = mock(ISpecification.class);

        when(mockPhase1.getSpecification()).thenReturn(dummySpecification);

        when(dummySpecification.copy()).thenReturn(dummySpecification);
        when(mockPhase1.copy(dummySpecification)).thenReturn(mockPhase1);
        when(mockPhase2.copy(dummySpecification)).thenReturn(mockPhase2);

        List<IPhase> phases = List.of(mockPhase1, mockPhase2);
        Map<String, IEngine> engines = new HashMap<>();
        engines.put("testEngine", mockEngine);
        MonteCarloSimulation simulation = new MonteCarloSimulation(engines, "testEngine");

        int runs = 10;
        List<IRunResult> results = simulation.run(runs, phases);
        assertEquals(runs, results.size(), "The number of results should equal the number of runs.");
        verify(mockEngine, times(runs)).simulatePhases(anyList());
    }

    @Test
    public void testRunWithZeroRunsReturnsEmptyList() throws Exception {
        IEngine mockEngine = mock(IEngine.class);
        IRunResult dummyResult = mock(IRunResult.class);
        when(mockEngine.simulatePhases(anyList())).thenReturn(dummyResult);

        IPhase mockPhase1 = mock(IPhase.class);
        IPhase mockPhase2 = mock(IPhase.class);
        ISpecification dummySpecification = mock(ISpecification.class);

        when(mockPhase1.getSpecification()).thenReturn(dummySpecification);
        when(dummySpecification.copy()).thenReturn(dummySpecification);
        when(mockPhase1.copy(dummySpecification)).thenReturn(mockPhase1);
        when(mockPhase2.copy(dummySpecification)).thenReturn(mockPhase2);

        List<IPhase> phases = List.of(mockPhase1, mockPhase2);
        Map<String, IEngine> engines = new HashMap<>();
        engines.put("testEngine", mockEngine);
        MonteCarloSimulation simulation = new MonteCarloSimulation(engines, "testEngine");

        int runs = 0;
        List<IRunResult> results = simulation.run(runs, phases);
        assertTrue(results.isEmpty(), "The results list should be empty when no runs are executed.");
        verify(mockEngine, never()).simulatePhases(anyList());
    }

    @Test
    public void testEngineSimulationFailure() throws Exception {
        IEngine mockEngine = mock(IEngine.class);
        IRunResult dummyResult = mock(IRunResult.class);

        when(mockEngine.simulatePhases(anyList()))
                .thenReturn(dummyResult)
                .thenThrow(new RuntimeException("Simulation error"));

        IPhase mockPhase1 = mock(IPhase.class);
        IPhase mockPhase2 = mock(IPhase.class);
        ISpecification dummySpec = mock(ISpecification.class);

        when(mockPhase1.getSpecification()).thenReturn(dummySpec);
        when(dummySpec.copy()).thenReturn(dummySpec);
        when(mockPhase1.copy(dummySpec)).thenReturn(mockPhase1);
        when(mockPhase2.copy(dummySpec)).thenReturn(mockPhase2);

        List<IPhase> phases = List.of(mockPhase1, mockPhase2);
        Map<String, IEngine> engines = new HashMap<>();
        engines.put("testEngine", mockEngine);
        MonteCarloSimulation simulation = new MonteCarloSimulation(engines, "testEngine");

        List<IRunResult> results = simulation.run(2, phases);
        assertEquals(1, results.size(), "Only successful runs should be added to the results list.");
        verify(mockEngine, times(2)).simulatePhases(anyList());
    }

    @Test
    public void testPhaseCopyInvocation() throws Exception {
        IEngine mockEngine = mock(IEngine.class);
        IRunResult dummyResult = mock(IRunResult.class);
        when(mockEngine.simulatePhases(any())).thenReturn(dummyResult);

        IPhase phase1 = mock(IPhase.class);
        IPhase phase2 = mock(IPhase.class);
        ISpecification dummySpec = mock(ISpecification.class);
        ISpecification specCopy = mock(ISpecification.class);

        when(phase1.getSpecification()).thenReturn(dummySpec);
        when(dummySpec.copy()).thenReturn(specCopy);
        when(phase1.copy(any())).thenReturn(phase1);
        when(phase2.copy(any())).thenReturn(phase2);

        Map<String, IEngine> engines = new HashMap<>();
        engines.put("testEngine", mockEngine);
        MonteCarloSimulation simulation = new MonteCarloSimulation(engines, "testEngine");
        List<IPhase> phases = List.of(phase1, phase2);
        int runs = 3;
        simulation.run(runs, phases);

        verify(phase1, times(runs)).getSpecification();
        verify(dummySpec, times(runs)).copy();
        verify(phase1, times(runs)).copy(specCopy);
        verify(phase2, times(runs)).copy(specCopy);

        ArgumentCaptor<ISpecification> specCaptor = ArgumentCaptor.forClass(ISpecification.class);
        verify(phase2, atLeast(1)).copy(specCaptor.capture());
        specCaptor.getAllValues().forEach(capturedSpec ->
                assertEquals(specCopy, capturedSpec, "Each phase copy should be called with the same specification copy."));
    }

    @Test
    public void testResultsResetBetweenRuns() throws Exception {
        IEngine mockEngine = mock(IEngine.class);
        IRunResult dummyResult = mock(IRunResult.class);
        when(mockEngine.simulatePhases(any())).thenReturn(dummyResult);

        IPhase phase1 = mock(IPhase.class);
        IPhase phase2 = mock(IPhase.class);
        ISpecification dummySpec = mock(ISpecification.class);
        ISpecification specCopy = mock(ISpecification.class);
        when(phase1.getSpecification()).thenReturn(dummySpec);

        when(dummySpec.copy()).thenReturn(specCopy);
        when(phase1.copy(any())).thenReturn(phase1);
        when(phase2.copy(any())).thenReturn(phase2);

        Map<String, IEngine> engines = new HashMap<>();
        engines.put("testEngine", mockEngine);
        MonteCarloSimulation simulation = new MonteCarloSimulation(engines, "testEngine");

        List<IPhase> phases = List.of(phase1, phase2);
        int firstRunCount = 2;
        List<IRunResult> firstResults = simulation.run(firstRunCount, phases);
        assertEquals(firstRunCount, firstResults.size(), "First run should return 2 results.");

        int secondRunCount = 3;
        List<IRunResult> secondResults = simulation.run(secondRunCount, phases);
        assertEquals(secondRunCount, secondResults.size(), "Second run should return 3 results only, not accumulated from first run.");
    }

    @Test
    public void testEmptyPhasesListThrowsException() {
        IEngine mockEngine = mock(IEngine.class);
        Map<String, IEngine> engines = new HashMap<>();
        engines.put("testEngine", mockEngine);

        MonteCarloSimulation simulation = new MonteCarloSimulation(engines, "testEngine");

        List<IPhase> emptyPhases = Collections.emptyList();
        List<IPhase> notEmptyPhases = new ArrayList<>();
        notEmptyPhases.add(mock(IPhase.class));

        assertThrows(IllegalArgumentException.class, () -> {
            simulation.run(10, emptyPhases);
        }, "Expected run() to throw IndexOutOfBoundsException when phases list is empty.");
        assertThrows(IllegalArgumentException.class, () -> {
            simulation.run(-10, notEmptyPhases);
        }, "Expected run() to throw IndexOutOfBoundsException when phases list is empty.");
    }

    @Test
    public void testTaskSubmissionCount() throws Exception {
        IEngine mockEngine = mock(IEngine.class);
        IRunResult dummyResult = mock(IRunResult.class);
        when(mockEngine.simulatePhases(any())).thenReturn(dummyResult);

        IPhase phase1 = mock(IPhase.class);
        IPhase phase2 = mock(IPhase.class);
        ISpecification dummySpec = mock(ISpecification.class);
        when(phase1.getSpecification()).thenReturn(dummySpec);
        when(dummySpec.copy()).thenReturn(dummySpec);
        when(phase1.copy(dummySpec)).thenReturn(phase1);
        when(phase2.copy(dummySpec)).thenReturn(phase2);
        List<IPhase> phases = List.of(phase1, phase2);

        Map<String, IEngine> engines = new HashMap<>();
        engines.put("testEngine", mockEngine);
        MonteCarloSimulation simulation = new MonteCarloSimulation(engines, "testEngine");

        CountingExecutorService countingExecutor = new CountingExecutorService(2);

        Field executorField = MonteCarloSimulation.class.getDeclaredField("executorService");
        executorField.setAccessible(true);
        executorField.set(simulation, countingExecutor);

        int runs = 5;
        simulation.run(runs, phases);

        assertEquals(runs, countingExecutor.getSubmitCount(), "The submit count should equal the number of runs");
        countingExecutor.shutdown();
    }


    @Test
    void testMultipleSuccessfulRuns() throws Exception {
        IEngine engine = mock(IEngine.class);
        IPhase phase = mock(IPhase.class);
        ISpecification specification = mock(ISpecification.class);
        IRunResult result = mock(IRunResult.class);

        when(phase.getSpecification()).thenReturn(specification);
        when(specification.copy()).thenReturn(specification);
        when(phase.copy(specification)).thenReturn(phase);
        when(engine.simulatePhases(anyList())).thenReturn(result);

        Map<String, IEngine> engines = new HashMap<>();
        engines.put("scheduleEngine", engine);

        MonteCarloSimulation simulation = new MonteCarloSimulation(engines, "scheduleEngine");

        int runs = 10000;
        List<IPhase> phases = Collections.singletonList(phase);
        List<IRunResult> results = simulation.run(runs, phases);
        verify(engine, times(runs)).simulatePhases(anyList());
        assertEquals(runs, results.size());
    }

    @Test
    void testConcurrentExecutionHandling() throws Exception {
        IEngine engine = mock(IEngine.class);
        IPhase phase = mock(IPhase.class);
        ISpecification specification = mock(ISpecification.class);
        IRunResult result = mock(IRunResult.class);

        when(phase.getSpecification()).thenReturn(specification);
        when(specification.copy()).thenReturn(specification);
        when(phase.copy(specification)).thenReturn(phase);
        ConcurrentLinkedQueue<String> threadNames = new ConcurrentLinkedQueue<>();

        when(engine.simulatePhases(anyList())).thenAnswer((Answer<IRunResult>) invocation -> {
            Thread.sleep(50);
            threadNames.add(Thread.currentThread().getName());
            return result;
        });

        Map<String, IEngine> engines = new HashMap<>();
        engines.put("scheduleEngine", engine);

        MonteCarloSimulation simulation = new MonteCarloSimulation(engines, "scheduleEngine");
        int runs = 20;
        List<IPhase> phases = Collections.singletonList(phase);
        List<IRunResult> results = simulation.run(runs, phases);
        verify(engine, times(runs)).simulatePhases(anyList());

        assertEquals(runs, results.size(), "The results list should have one entry per simulation run.");
        assertFalse(threadNames.isEmpty(), "There should be recorded thread names.");

        long distinctThreads = threadNames.stream().distinct().count();
        assertTrue(distinctThreads > 1, "Expected tasks to run on more than one thread for concurrent execution.");
    }

    @Test
    void testResultVerificationForEachRun() throws Exception {
        IEngine engine = mock(IEngine.class);
        IPhase phase = mock(IPhase.class);
        ISpecification specification = mock(ISpecification.class);
        when(phase.getSpecification()).thenReturn(specification);
        when(specification.copy()).thenReturn(specification);
        when(phase.copy(specification)).thenReturn(phase);

        int runs = 10000;

        List<IRunResult> distinctResults = new ArrayList<>();
        for (int i = 0; i < runs; i++) {
            distinctResults.add(mock(IRunResult.class, "result" + i));
        }

        AtomicInteger counter = new AtomicInteger(0);
        when(engine.simulatePhases(anyList()))
                .thenAnswer((Answer<IRunResult>) invocation -> {
                    int index = counter.getAndIncrement();
                    return distinctResults.get(index);
                });

        Map<String, IEngine> engines = new HashMap<>();
        engines.put("scheduleEngine", engine);

        MonteCarloSimulation simulation = new MonteCarloSimulation(engines, "scheduleEngine");

        List<IPhase> phases = Collections.singletonList(phase);
        List<IRunResult> results = simulation.run(runs, phases);
        verify(engine, times(runs)).simulatePhases(anyList());

        assertEquals(runs, results.size(), "The results list size should match the number of runs.");
        assertEquals(new HashSet<>(distinctResults), new HashSet<>(results),
                "The results list should contain exactly the distinct IResult mocks.");
    }
}
