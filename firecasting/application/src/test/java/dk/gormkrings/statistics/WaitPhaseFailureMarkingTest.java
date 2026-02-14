package dk.gormkrings.statistics;

import dk.gormkrings.result.IRunResult;
import dk.gormkrings.simulation.IProgressCallback;
import dk.gormkrings.simulation.data.LiveData;
import dk.gormkrings.simulation.result.RunResult;
import dk.gormkrings.simulation.result.Snapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WaitPhaseFailureMarkingTest {

    private static IProgressCallback noopProgress() {
        return __ -> {
            // no-op
        };
    }

    @Test
    void passiveWaitWithZeroCapital_doesNotMarkRunAsFailed() {
        SimulationAggregationService agg = new SimulationAggregationService();

        RunResult run = new RunResult();
        long startEpochDay = 0; // 1900-01-01; year grouping doesn't matter for this test

        LiveData s0 = new LiveData(startEpochDay);
        s0.setPhaseName("Passive");
        run.addSnapshot(new Snapshot(s0));

        LiveData s1 = new LiveData(startEpochDay);
        s1.incrementTime(31);
        s1.setPhaseName("Passive");
        run.addSnapshot(new Snapshot(s1));

        LiveData s2 = new LiveData(startEpochDay);
        s2.incrementTime(62);
        s2.setPhaseName("Passive");
        run.addSnapshot(new Snapshot(s2));

        List<YearlySummary> summaries = agg.aggregateResults(List.of(run), "test", noopProgress());
        YearlySummary passive = summaries.stream()
                .filter(s -> "Passive".equalsIgnoreCase(s.getPhaseName()))
                .findFirst()
                .orElse(null);

        assertNotNull(passive);
        assertEquals(0.0, passive.getNegativeCapitalPercentage(), 1e-9);
    }

    @Test
    void withdrawWithZeroCapital_stillMarksRunAsFailed() {
        SimulationAggregationService agg = new SimulationAggregationService();

        RunResult run = new RunResult();
        long startEpochDay = 0;

        LiveData s0 = new LiveData(startEpochDay);
        s0.setPhaseName("Withdraw");
        run.addSnapshot(new Snapshot(s0));

        List<YearlySummary> summaries = agg.aggregateResults(List.of(run), "test", noopProgress());
        YearlySummary withdraw = summaries.stream()
                .filter(s -> "Withdraw".equalsIgnoreCase(s.getPhaseName()))
                .findFirst()
                .orElse(null);

        assertNotNull(withdraw);
        assertEquals(100.0, withdraw.getNegativeCapitalPercentage(), 1e-9);
    }

    @Test
    void afterHavingCapital_zeroCapitalMarksFailureOutsideDeposit() {
        SimulationAggregationService agg = new SimulationAggregationService();

        IRunResult run = new RunResult();
        long startEpochDay = 0;

        LiveData deposit = new LiveData(startEpochDay);
        deposit.setPhaseName("Deposit");
        deposit.addToCapital(100.0);
        run.addSnapshot(new Snapshot(deposit));

        LiveData passiveZero = new LiveData(startEpochDay);
        passiveZero.incrementTime(31);
        passiveZero.setPhaseName("Passive");
        // capital stays 0
        run.addSnapshot(new Snapshot(passiveZero));

        List<YearlySummary> summaries = agg.aggregateResults(List.of(run), "test", noopProgress());
        YearlySummary passive = summaries.stream()
                .filter(s -> "Passive".equalsIgnoreCase(s.getPhaseName()))
                .findFirst()
                .orElse(null);

        assertNotNull(passive);
        assertEquals(100.0, passive.getNegativeCapitalPercentage(), 1e-9);
    }
}
