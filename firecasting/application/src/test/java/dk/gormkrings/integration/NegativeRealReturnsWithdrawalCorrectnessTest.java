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
import dk.gormkrings.result.IRunResult;
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

class NegativeRealReturnsWithdrawalCorrectnessTest {

    @Test
    void multiYearNegativeRealReturns_withInflationAdjustedSpending_appliesWithdrawalsCorrectly() {
        // Arrange
        final double initialBalance = 1_000_000.0;
        final double baseMonthlySpendingReal = 1_000.0;

        // Nominal return sequence per year.
        // Years 1-4 are negative *real* (below inflation), years 5-6 are positive real.
        final double[] annualNominalReturns = new double[] {
                0.02, 0.02,   // below inflation => negative real
                -0.05, -0.05, // definitely negative real
                0.08, 0.08    // positive real
        };

        final double annualInflationFactor = 1.05; // 5% inflation compounded at YEAR_END

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

        IReturner sequencedReturner = new SequencedAnnualReturner(annualNominalReturns);

        IInflation fixedInflation = new IInflation() {
            @Override
            public double calculateInflation() {
                return annualInflationFactor;
            }

            @Override
            public IInflation copy() {
                return this;
            }
        };

        ISpecification specification = new Specification(startDate.getEpochDay(), noTax, sequencedReturner, fixedInflation);
        ILiveData liveData = (ILiveData) specification.getLiveData();
        liveData.addToCapital(initialBalance);
        liveData.addToDeposited(initialBalance);

        // Fixed real spending, indexed by inflation inside Withdraw.
        Withdraw withdrawAction = new Withdraw(baseMonthlySpendingReal, 0.0, 0.0, 0.0);

        int runYears = annualNominalReturns.length;
        IDate endDate = startDate.plusMonths(12 * runYears);
        long durationDays = startDate.daysUntil(endDate);

        List<Double> monthEndWithdrawals = new ArrayList<>();
        List<Double> monthEndCapitals = new ArrayList<>();
        List<Double> monthEndInflationIdx = new ArrayList<>();

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
                ILiveData ld = getLiveData();
                monthEndWithdrawals.add(ld.getWithdraw());
                monthEndCapitals.add(ld.getCapital());
                monthEndInflationIdx.add(ld.getInflation());
            }

            @Override
            public WithdrawCallPhase copy(ISpecification specificationCopy) {
                // Single-threaded deterministic test.
                return this;
            }
        };

        List<IPhase> phases = List.of(withdrawPhase);

        // Act
        engine.init(phases);
        IRunResult runResult = engine.simulatePhases(new LinkedList<>(phases));

        // Assert
        int expectedMonths = runYears * 12;
        assertEquals(expectedMonths, monthEndWithdrawals.size(), "Expected one withdrawal per month-end");
        assertEquals(expectedMonths, monthEndCapitals.size(), "Expected one capital record per month-end");

        // Withdrawals should be the base monthly real spending multiplied by the inflation index,
        // where inflation is compounded at YEAR_END (so it steps up after month 12, 24, ...).
        for (int m = 1; m <= expectedMonths; m++) {
            int completedYearsBeforeThisMonth = (m - 1) / 12;
            double expectedInflationIdx = Math.pow(annualInflationFactor, completedYearsBeforeThisMonth);
            double expectedWithdraw = baseMonthlySpendingReal * expectedInflationIdx;

            double actualInflationIdx = monthEndInflationIdx.get(m - 1);
            double actualWithdraw = monthEndWithdrawals.get(m - 1);

            assertEquals(expectedInflationIdx, actualInflationIdx, 1e-9,
                    "Inflation index mismatch at month " + m);
            assertEquals(expectedWithdraw, actualWithdraw, 1e-6,
                    "Withdrawal mismatch at month " + m);
            assertTrue(monthEndCapitals.get(m - 1) >= -1e-9,
                    "Capital went negative at month " + m + ": " + monthEndCapitals.get(m - 1));
        }

        // Sanity: the run includes a multi-year negative real return regime.
        // Use YEAR_END snapshots (which occur after inflation compounding) and assert real capital declines early.
        // Note: snapshots are also added at sim start; year-end snapshots follow.
        var snapshots = runResult.getSnapshots();
        assertTrue(snapshots.size() >= 1 + runYears, "Expected sim-start + yearly snapshots");

        double real0 = snapshots.get(0).getState().getCapital() / snapshots.get(0).getState().getInflation();
        double realYear1 = snapshots.get(1).getState().getCapital() / snapshots.get(1).getState().getInflation();
        double realYear2 = snapshots.get(2).getState().getCapital() / snapshots.get(2).getState().getInflation();

        assertTrue(realYear1 < real0, "Expected real capital to decline in year 1 under negative real returns");
        assertTrue(realYear2 < realYear1, "Expected real capital to decline in year 2 under negative real returns");
    }

    /**
     * Deterministic returner that applies a fixed annual nominal return per year,
     * converted to a constant monthly rate for that year.
     */
    private static final class SequencedAnnualReturner implements IReturner {
        private final double[] annualNominalReturns;
        private int monthIndex = 0;

        private SequencedAnnualReturner(double[] annualNominalReturns) {
            this.annualNominalReturns = annualNominalReturns.clone();
        }

        @Override
        public double calculateReturn(double amount) {
            if (amount <= 0.0) {
                monthIndex++;
                return 0.0;
            }

            int yearIndex = Math.min(monthIndex / 12, annualNominalReturns.length - 1);
            double annual = annualNominalReturns[yearIndex];
            double monthlyRate = Math.pow(1.0 + annual, 1.0 / 12.0) - 1.0;

            monthIndex++;
            return amount * monthlyRate;
        }

        @Override
        public void onMonthEnd() {
            // no-op
        }

        @Override
        public IReturner copy() {
            return new SequencedAnnualReturner(annualNominalReturns);
        }
    }
}
