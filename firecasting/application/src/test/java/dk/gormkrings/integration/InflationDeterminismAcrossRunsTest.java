package dk.gormkrings.integration;

import dk.gormkrings.action.Deposit;
import dk.gormkrings.action.Passive;
import dk.gormkrings.calendar.WeekdayTradingCalendar;
import dk.gormkrings.data.IDate;
import dk.gormkrings.data.ILiveData;
import dk.gormkrings.factory.IDateFactory;
import dk.gormkrings.factory.IResultFactory;
import dk.gormkrings.factory.ISnapshotFactory;
import dk.gormkrings.inflation.DefaultInflation;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.phase.callBased.DepositCallPhase;
import dk.gormkrings.phase.callBased.PassiveCallPhase;
import dk.gormkrings.result.IRunResult;
import dk.gormkrings.result.ISnapshot;
import dk.gormkrings.returns.SimpleDailyReturn;
import dk.gormkrings.simulation.ReturnStep;
import dk.gormkrings.simulation.engine.schedule.DefaultScheduleFactory;
import dk.gormkrings.simulation.engine.schedule.ScheduleEngine;
import dk.gormkrings.simulation.factory.DefaultDateFactory;
import dk.gormkrings.simulation.factory.DefaultResultFactory;
import dk.gormkrings.simulation.factory.DefaultSnapshotFactory;
import dk.gormkrings.simulation.monteCarlo.MonteCarloSimulation;
import dk.gormkrings.simulation.specification.Specification;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.ITaxExemption;
import dk.gormkrings.tax.ITaxRule;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InflationDeterminismAcrossRunsTest {

    @Test
    void inflation_is_identical_across_monte_carlo_runs() {
        final int years = 12;
        final int runs = 50;

        // Guard: if inflation ever becomes stochastic per run, this test should fail.
        final double annualInflationFactor = 1.07;

        // Keep returns deterministic so the test isolates inflation behavior.
        final double annualNominalReturn = 0.0;

        IDateFactory dateFactory = new DefaultDateFactory();
        IResultFactory resultFactory = new DefaultResultFactory();
        ISnapshotFactory snapshotFactory = new DefaultSnapshotFactory();

        var scheduleFactory = new DefaultScheduleFactory(dateFactory);
        var engine = new ScheduleEngine(resultFactory, snapshotFactory, scheduleFactory);

        IDate startDate = dateFactory.dateOf(2020, 1, 1);

        ITaxRule noTax = new ITaxRule() {
            @Override
            public double calculateTax(double amount) {
                return 0.0;
            }

            @Override
            public ITaxRule copy() {
                return this;
            }
        };

        SimpleDailyReturn returner = new SimpleDailyReturn(ReturnStep.MONTHLY);
        returner.setAveragePercentage((float) annualNominalReturn);

        ISpecification specification = new Specification(
                startDate.getEpochDay(),
                noTax,
                returner,
                new DefaultInflation(annualInflationFactor)
        );

        DepositCallPhase initialDeposit = new DepositCallPhase(
                specification,
                startDate,
                List.<ITaxExemption>of(),
                0L,
                new Deposit(100_000.0, 0.0, 0.0),
                ReturnStep.MONTHLY,
                new WeekdayTradingCalendar()
        );

        IDate endDate = startDate.plusMonths(12 * years);
        long durationDays = startDate.daysUntil(endDate);
        PassiveCallPhase passive = new PassiveCallPhase(
                specification,
                startDate,
                List.<ITaxExemption>of(),
                durationDays,
                new Passive(),
                ReturnStep.MONTHLY,
                new WeekdayTradingCalendar()
        );

        List<IPhase> phases = List.of(initialDeposit, passive);

        ExecutorService pool = Executors.newFixedThreadPool(1);
        try {
            Map<String, dk.gormkrings.engine.IEngine> engines = Map.of("scheduleEngine", engine);
            MonteCarloSimulation sim = new MonteCarloSimulation(engines, "scheduleEngine", pool, 1_000_000, true);

            List<IRunResult> results = sim.run(runs, new LinkedList<>(phases));
            assertEquals(runs, results.size(), "Expected one result per run");

            for (int runIdx = 0; runIdx < runs; runIdx++) {
                List<ISnapshot> snapshots = results.get(runIdx).getSnapshots();
                assertEquals(years + 1, snapshots.size(), "Run " + runIdx + ": expected one start snapshot plus one per year end");

                for (int year = 0; year <= years; year++) {
                    ILiveData state = snapshots.get(year).getState();
                    double expectedInflationIndex = Math.pow(annualInflationFactor, year);
                    assertClose(expectedInflationIndex, state.getInflation(), "inflation", runIdx, year);
                }
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private static void assertClose(double expected, double actual, String field, int runIdx, int year) {
        double absTol = 1e-9;
        double relTol = 1e-12;
        double tol = Math.max(absTol, Math.abs(expected) * relTol);
        assertEquals(expected, actual, tol, field + " mismatch (run=" + runIdx + ", year=" + year + ")");
    }
}
