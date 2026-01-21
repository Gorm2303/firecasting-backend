package dk.gormkrings.simulation;

import dk.gormkrings.queue.SimulationQueueService;
import dk.gormkrings.sse.SimulationSseService;
import dk.gormkrings.statistics.StatisticsService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SimulationStartServiceSeedTest {

    @Test
    void negativeSeedSkipsDedupAndCreatesNewRun() {
        SimulationQueueService queue = mock(SimulationQueueService.class);
        SimulationRunner runner = mock(SimulationRunner.class);
        SimulationSseService sse = mock(SimulationSseService.class);
        StatisticsService stats = mock(StatisticsService.class);
        SimulationResultsCache cache = mock(SimulationResultsCache.class);

        when(queue.submitWithId(anyString(), any())).thenReturn(true);

        SimulationStartService svc = new SimulationStartService(queue, runner, sse, stats, cache);

        // Inject config values (no Spring here)
        TestUtil.setField(svc, "runs", 1);
        TestUtil.setField(svc, "batchSize", 1);

        var rc = new dk.gormkrings.returns.ReturnerConfig();
        rc.setSeed(-1L);
        var spec = new SimulationRunSpec(
                new dk.gormkrings.simulation.data.Date(0),
                List.of(),
                "Capital",
                0.0f,
                "dataDrivenReturn",
                1.02D,
                rc,
                null
        );

        var resp = svc.startSimulation("/start", spec, Map.of("any", "input"));

        assertEquals(202, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().containsKey("id"));

        verify(stats, never()).findExistingRunIdForInput(any());
        verify(queue, times(1)).submitWithId(anyString(), any());
    }

    @Test
    void nonNegativeSeedAllowsDedupHit() {
        SimulationQueueService queue = mock(SimulationQueueService.class);
        SimulationRunner runner = mock(SimulationRunner.class);
        SimulationSseService sse = mock(SimulationSseService.class);
        StatisticsService stats = mock(StatisticsService.class);
        SimulationResultsCache cache = mock(SimulationResultsCache.class);

        when(stats.findExistingRunIdForInput(any())).thenReturn(Optional.of("existing-id"));

        SimulationStartService svc = new SimulationStartService(queue, runner, sse, stats, cache);

        TestUtil.setField(svc, "runs", 1);
        TestUtil.setField(svc, "batchSize", 1);

        var rc = new dk.gormkrings.returns.ReturnerConfig();
        rc.setSeed(123L);
        var spec = new SimulationRunSpec(
                new dk.gormkrings.simulation.data.Date(0),
                List.of(),
                "Capital",
                0.0f,
                "dataDrivenReturn",
                1.02D,
                rc,
                null
        );

        var resp = svc.startSimulation("/start", spec, Map.of("any", "input"));

        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertEquals("existing-id", resp.getBody().get("id"));

        verify(stats, times(1)).findExistingRunIdForInput(any());
        verify(queue, never()).submitWithId(anyString(), any());
    }

    /** Minimal reflection helper to set @Value fields in unit tests without Spring. */
    static final class TestUtil {
        static void setField(Object target, String fieldName, Object value) {
            try {
                var f = target.getClass().getDeclaredField(fieldName);
                f.setAccessible(true);
                f.set(target, value);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
