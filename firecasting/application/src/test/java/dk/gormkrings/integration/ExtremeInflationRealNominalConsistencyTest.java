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
import dk.gormkrings.result.ISnapshot;
import dk.gormkrings.returns.SimpleDailyReturn;
import dk.gormkrings.simulation.ReturnStep;
import dk.gormkrings.simulation.engine.schedule.DefaultScheduleFactory;
import dk.gormkrings.simulation.engine.schedule.ScheduleEngine;
import dk.gormkrings.simulation.factory.DefaultDateFactory;
import dk.gormkrings.simulation.factory.DefaultResultFactory;
import dk.gormkrings.simulation.factory.DefaultSnapshotFactory;
import dk.gormkrings.simulation.specification.Specification;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.ITaxExemption;
import dk.gormkrings.tax.ITaxRule;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExtremeInflationRealNominalConsistencyTest {

    @Test
    void extremeInflation_withZeroNominalReturn_preservesNominalButReducesReal_consistently() {
        // Arrange
        final int years = 10;
        final double initialBalance = 100_000.0;

        // 25% inflation compounded at YEAR_END.
        final double annualInflationFactor = 1.25;

        // 0% nominal return so nominal capital should remain constant.
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

        // Phase 1: one-time deposit.
        DepositCallPhase initialDeposit = new DepositCallPhase(
                specification,
                startDate,
                List.<ITaxExemption>of(),
                0L,
                new Deposit(initialBalance, 0.0, 0.0),
                ReturnStep.MONTHLY,
                new WeekdayTradingCalendar()
        );

        // Phase 2: passive for N years.
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

        // Act
        engine.init(phases);
        var result = engine.simulatePhases(new LinkedList<>(phases));

        // Assert
        List<ISnapshot> snapshots = result.getSnapshots();
        assertEquals(years + 1, snapshots.size(), "Expected one start snapshot plus one snapshot per year end");

        for (int year = 0; year <= years; year++) {
            ILiveData state = snapshots.get(year).getState();

            double expectedNominalCapital = initialBalance;
            double expectedInflationIndex = Math.pow(annualInflationFactor, year);
            double expectedRealCapital = expectedNominalCapital / expectedInflationIndex;

            assertClose(expectedNominalCapital, state.getCapital(), "capital", year);
            assertClose(expectedInflationIndex, state.getInflation(), "inflation", year);
            assertClose(expectedRealCapital, state.getCapital() / state.getInflation(), "realCapital", year);
        }
    }

    private static void assertClose(double expected, double actual, String field, int year) {
        double absTol = 1e-3;
        double relTol = 1e-8;
        double tol = Math.max(absTol, Math.abs(expected) * relTol);
        assertEquals(expected, actual, tol, field + " mismatch at year " + year);
    }
}
