package dk.gormkrings.integration;

import dk.gormkrings.action.Passive;
import dk.gormkrings.calendar.WeekdayTradingCalendar;
import dk.gormkrings.data.IDate;
import dk.gormkrings.data.ILiveData;
import dk.gormkrings.fee.IYearlyFee;
import dk.gormkrings.factory.IDateFactory;
import dk.gormkrings.factory.IResultFactory;
import dk.gormkrings.factory.ISnapshotFactory;
import dk.gormkrings.inflation.IInflation;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.phase.callBased.PassiveCallPhase;
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

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FeeExceedsReturnsPortfolioDeclinesTest {

    @Test
    void annualFeeGreaterThanExpectedReturn_noSpending_capitalDeclines_andFeesAreRecorded() {
        // Arrange
        final double initialBalance = 1_000_000.0;
        final double annualNominalReturn = 0.02; // +2%
        final double annualFeePct = 3.0;         // 3% fee (pct points), larger than expected return
        final int years = 3;

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

        // Constant annual return converted to a constant monthly rate.
        IReturner fixedReturner = new IReturner() {
            @Override
            public double calculateReturn(double amount) {
                if (amount <= 0.0) return 0.0;
                double monthlyRate = Math.pow(1.0 + annualNominalReturn, 1.0 / 12.0) - 1.0;
                return amount * monthlyRate;
            }

            @Override
            public IReturner copy() {
                return this;
            }
        };

        IYearlyFee yearlyFee = new IYearlyFee() {
            @Override
            public double calculateFee(double capital) {
                if (capital <= 0.0) return 0.0;
                return capital * (annualFeePct / 100.0);
            }

            @Override
            public IYearlyFee copy() {
                return this;
            }
        };

        ISpecification specification = new Specification(startDate.getEpochDay(), noTax, fixedReturner, noInflation, yearlyFee);
        ILiveData liveData = (ILiveData) specification.getLiveData();
        liveData.addToCapital(initialBalance);
        liveData.addToDeposited(initialBalance);

        IDate endDate = startDate.plusMonths(12 * years);
        long durationDays = startDate.daysUntil(endDate);

        // Passive phase: no withdrawals, only returns + fee at YEAR_END.
        IPhase phase = new PassiveCallPhase(
                specification,
                startDate,
                List.<ITaxExemption>of(),
                durationDays,
                new Passive(),
                ReturnStep.MONTHLY,
                new WeekdayTradingCalendar()
        ) {
            @Override
            public PassiveCallPhase copy(ISpecification specificationCopy) {
                return this;
            }
        };

        List<IPhase> phases = List.of(phase);

        // Act
        engine.init(phases);
        IRunResult result = engine.simulatePhases(new LinkedList<>(phases));

        // Assert
        var snapshots = result.getSnapshots();
        assertTrue(snapshots.size() >= 1 + years, "Expected sim-start + yearly snapshots");

        double startCapital = snapshots.get(0).getState().getCapital();
        double endCapital = snapshots.get(years).getState().getCapital();
        assertTrue(endCapital < startCapital, "Expected capital to decline when fee exceeds expected return");

        double endFees = snapshots.get(years).getState().getFee();
        assertTrue(endFees > 0.0, "Expected fees to be recorded explicitly (fee impact > 0)");
    }
}
