package dk.gormkrings.simulation.monteCarlo;

import dk.gormkrings.engine.IEngine;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.result.IRunResult;
import dk.gormkrings.specification.ISpecification;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MonteCarloSimulationTest {

    private MonteCarloSimulation newSim(IEngine engine, ExecutorService pool, int progressStep) {
        String name = "testEngine";
        Map<String, IEngine> engines = Map.of(name, engine);
        return new MonteCarloSimulation(engines, name, pool, progressStep, true);
    }

    private static List<IPhase> phasesWithSharedSpec(IPhase... phases) {
        // each run must use a fresh spec copy; we just wire mocks so copy() returns same mock
        ISpecification base = mock(ISpecification.class);
        when(base.copy()).thenReturn(base);
        for (IPhase p : phases) {
            when(p.getSpecification()).thenReturn(base);
            when(p.copy(base)).thenReturn(p);
        }
        return Arrays.asList(phases);
    }

    @Test
    void validEngineSelection() {
        IEngine engine = mock(IEngine.class);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            assertDoesNotThrow(() -> newSim(engine, pool, 1000));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void invalidEngineNameThrows() {
        IEngine engine = mock(IEngine.class);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            // name in map doesn't match name we pass
            Map<String, IEngine> engines = Map.of("someOtherEngine", engine);
            assertThrows(IllegalArgumentException.class,
                    () -> new MonteCarloSimulation(engines, "testEngine", pool, 1000, true));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void runReturnsCorrectNumberOfResults() throws Exception {
        IEngine engine = mock(IEngine.class);
        IRunResult result = mock(IRunResult.class);
        when(engine.simulatePhases(anyList())).thenReturn(result);

        IPhase p1 = mock(IPhase.class), p2 = mock(IPhase.class);
        List<IPhase> phases = phasesWithSharedSpec(p1, p2);

        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            MonteCarloSimulation sim = newSim(engine, pool, 1000);
            int runs = 10;

            List<IRunResult> out = sim.run(runs, phases);

            assertEquals(runs, out.size());
            verify(engine, times(runs)).simulatePhases(anyList());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void zeroRunsThrows_and_emptyPhasesThrow() {
        IEngine engine = mock(IEngine.class);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            MonteCarloSimulation sim = newSim(engine, pool, 1000);

            List<IPhase> phases = phasesWithSharedSpec(mock(IPhase.class));
            assertThrows(IllegalArgumentException.class, () -> sim.run(0, phases));
            assertThrows(IllegalArgumentException.class, () -> sim.run(-1, phases));
            assertThrows(IllegalArgumentException.class, () -> sim.run(10, Collections.emptyList()));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void engineFailurePropagatesAsRuntimeException() {
        IEngine engine = mock(IEngine.class);
        when(engine.simulatePhases(anyList()))
                .thenReturn(mock(IRunResult.class))
                .thenThrow(new RuntimeException("boom"));

        IPhase p = mock(IPhase.class);
        List<IPhase> phases = phasesWithSharedSpec(p);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            MonteCarloSimulation sim = newSim(engine, pool, 1);
            assertThrows(RuntimeException.class, () -> sim.run(2, phases));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void phaseCopyInvocationPerRun() throws Exception {
        IEngine engine = mock(IEngine.class);
        when(engine.simulatePhases(anyList())).thenReturn(mock(IRunResult.class));

        IPhase p1 = mock(IPhase.class), p2 = mock(IPhase.class);
        ISpecification base = mock(ISpecification.class);
        ISpecification copy = mock(ISpecification.class);
        when(p1.getSpecification()).thenReturn(base);
        when(base.copy()).thenReturn(copy);
        when(p1.copy(copy)).thenReturn(p1);
        when(p2.copy(copy)).thenReturn(p2);
        List<IPhase> phases = List.of(p1, p2);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            MonteCarloSimulation sim = newSim(engine, pool, 1000);
            int runs = 3;
            sim.run(runs, phases);

            verify(p1, times(runs)).getSpecification();
            verify(base, times(runs)).copy();
            verify(p1, times(runs)).copy(copy);
            verify(p2, times(runs)).copy(copy);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void resultsResetBetweenRuns() throws Exception {
        IEngine engine = mock(IEngine.class);
        when(engine.simulatePhases(anyList())).thenReturn(mock(IRunResult.class));

        List<IPhase> phases = phasesWithSharedSpec(mock(IPhase.class), mock(IPhase.class));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            MonteCarloSimulation sim = newSim(engine, pool, 1000);

            int first = 2;
            int second = 3;

            List<IRunResult> r1 = sim.run(first, phases);
            List<IRunResult> r2 = sim.run(second, phases);

            assertEquals(first, r1.size());
            assertEquals(second, r2.size());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void concurrentExecutionUsesMultipleThreads() throws Exception {
        IEngine engine = mock(IEngine.class);
        IRunResult res = mock(IRunResult.class);

        Set<String> threads = Collections.synchronizedSet(new HashSet<>());
        when(engine.simulatePhases(anyList())).thenAnswer((Answer<IRunResult>) inv -> {
            threads.add(Thread.currentThread().getName());
            Thread.sleep(30);
            return res;
        });

        List<IPhase> phases = phasesWithSharedSpec(mock(IPhase.class));

        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            MonteCarloSimulation sim = newSim(engine, pool, 1000);
            List<IRunResult> out = sim.run(20, phases);

            assertEquals(20, out.size());
            assertTrue(threads.size() > 1, "Expected more than one worker thread to be used");
        } finally {
            pool.shutdown();
            pool.awaitTermination(2, TimeUnit.SECONDS);
        }
    }

    @Test
    void returnsExactlyTheDistinctResults() throws Exception {
        IEngine engine = mock(IEngine.class);
        IPhase phase = mock(IPhase.class);
        List<IPhase> phases = phasesWithSharedSpec(phase);

        int runs = 1000;
        List<IRunResult> distinct = new ArrayList<>(runs);
        for (int i = 0; i < runs; i++) distinct.add(mock(IRunResult.class, "res" + i));

        AtomicInteger idx = new AtomicInteger(0);
        when(engine.simulatePhases(anyList()))
                .thenAnswer((Answer<IRunResult>) inv -> distinct.get(idx.getAndIncrement()));

        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            MonteCarloSimulation sim = newSim(engine, pool, 10_000);
            List<IRunResult> out = sim.run(runs, phases);

            assertEquals(runs, out.size());
            assertEquals(new HashSet<>(distinct), new HashSet<>(out));
        } finally {
            pool.shutdownNow();
        }
    }

}
