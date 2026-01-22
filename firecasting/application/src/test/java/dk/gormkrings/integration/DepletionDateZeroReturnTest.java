package dk.gormkrings.integration;

import dk.gormkrings.action.Withdraw;
import dk.gormkrings.calendar.WeekdayTradingCalendar;
import dk.gormkrings.data.IDate;
import dk.gormkrings.data.ILiveData;
import dk.gormkrings.factory.IDateFactory;
import dk.gormkrings.factory.IResultFactory;
import dk.gormkrings.factory.ISnapshotFactory;
import dk.gormkrings.inflation.IInflation;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.phase.callBased.WithdrawCallPhase;
import dk.gormkrings.returns.IReturner;
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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DepletionDateZeroReturnTest {

    @Test
    void constantAnnualSpending_withZeroReturns_depletesAtBalanceDivSpending_andNeverGoesNegative() {
        // Arrange
        final double initialBalance = 60_000.0;
        final double annualSpending = 12_000.0;
        final double monthlySpending = annualSpending / 12.0;

        final int expectedDepletionYears = (int) Math.round(initialBalance / annualSpending);
        assertEquals(5, expectedDepletionYears, "Test setup expects an exact integer depletion year");

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

        IReturner zeroReturner = new IReturner() {
            @Override
            public double calculateReturn(double amount) {
                return 0.0;
            }

            @Override
            public void onMonthEnd() {
                // no-op
            }

            @Override
            public IReturner copy() {
                return this;
            }
        };

        IInflation noInflation = new IInflation() {
            @Override
            public double calculateInflation() {
                return 1.0;
            }

            @Override
            public IInflation copy() {
                return this;
            }
        };

        ISpecification specification = new Specification(startDate.getEpochDay(), noTax, zeroReturner, noInflation);
        ILiveData liveData = (ILiveData) specification.getLiveData();
        liveData.addToCapital(initialBalance);
        liveData.addToDeposited(initialBalance);

        // Withdraw action is monthly amount (not percentage), with no dynamic variation.
        Withdraw withdrawAction = new Withdraw(monthlySpending, 0.0, 0.0, 0.0);

        // Run long enough that we're well past depletion.
        int runYears = 10;
        IDate endDate = startDate.plusMonths(12 * runYears);
        long durationDays = startDate.daysUntil(endDate);

        List<Double> capitalAfterEachMonthEnd = new ArrayList<>();

        IPhase withdrawPhase = new WithdrawCallPhase(
                specification,
                startDate,
                List.<ITaxExemption>of(),
                durationDays,
                withdrawAction,
                ReturnStep.MONTHLY,
                new WeekdayTradingCalendar()
        ) {
            @Override
            public void onMonthEnd() {
                super.onMonthEnd();
                capitalAfterEachMonthEnd.add(getLiveData().getCapital());
            }

            @Override
            public WithdrawCallPhase copy(ISpecification specificationCopy) {
                // This test runs on a single thread; keep copy simple and deterministic.
                return this;
            }
        };

        List<IPhase> phases = List.of(withdrawPhase);

        // Act
        engine.init(phases);
        engine.simulatePhases(new LinkedList<>(phases));

        // Assert
        int expectedDepletionMonths = expectedDepletionYears * 12;
        assertTrue(capitalAfterEachMonthEnd.size() >= expectedDepletionMonths,
                "Expected to record at least " + expectedDepletionMonths + " month-end values");

        // Never negative, and follows simple arithmetic until it hits 0.
        for (int m = 1; m <= expectedDepletionMonths; m++) {
            double expected = initialBalance - (monthlySpending * m);
            double actual = capitalAfterEachMonthEnd.get(m - 1);

            assertTrue(actual >= -1e-9, "Capital went negative at month " + m + ": " + actual);

            if (m < expectedDepletionMonths) {
                assertTrue(actual > 0.0, "Capital should be positive before depletion (month " + m + ")");
            }

            assertEquals(Math.max(0.0, expected), actual, 1e-6,
                    "Capital mismatch at month " + m);
        }

        // Depletion year should match the simple balance/spending arithmetic.
        int depletionMonthIndex1Based = firstMonthIndexWhereCapitalIsZero(capitalAfterEachMonthEnd);
        assertEquals(expectedDepletionMonths, depletionMonthIndex1Based, "Depletion month");

        int depletionYear = (int) Math.ceil(depletionMonthIndex1Based / 12.0);
        assertEquals(expectedDepletionYears, depletionYear, "Depletion year");

        // After depletion, balance stays at 0 (never becomes negative).
        for (int i = expectedDepletionMonths; i < capitalAfterEachMonthEnd.size(); i++) {
            double c = capitalAfterEachMonthEnd.get(i);
            assertTrue(c >= -1e-9, "Capital went negative after depletion at monthIndex=" + (i + 1));
            assertEquals(0.0, c, 1e-6, "Capital should remain 0 after depletion");
        }
    }

    private static int firstMonthIndexWhereCapitalIsZero(List<Double> capitals) {
        for (int i = 0; i < capitals.size(); i++) {
            if (Math.abs(capitals.get(i)) <= 1e-6) {
                return i + 1; // 1-based month count
            }
        }
        fail("Capital never reached zero");
        return -1;
    }
}
