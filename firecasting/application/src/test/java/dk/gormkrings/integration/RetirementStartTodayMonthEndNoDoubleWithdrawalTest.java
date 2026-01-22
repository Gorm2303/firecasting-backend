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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RetirementStartTodayMonthEndNoDoubleWithdrawalTest {

    @Test
    void startDateOnMonthEnd_doesNotTriggerImmediateWithdrawal() {
        // Arrange
        final double initialBalance = 1_000.0;
        final double monthlySpending = 100.0;

        IDateFactory dateFactory = new DefaultDateFactory();
        IResultFactory resultFactory = new DefaultResultFactory();
        ISnapshotFactory snapshotFactory = new DefaultSnapshotFactory();

        var scheduleFactory = new DefaultScheduleFactory(dateFactory);
        var engine = new ScheduleEngine(resultFactory, snapshotFactory, scheduleFactory);

        // Month-end start date (leap year makes this extra sensitive).
        IDate startDate = dateFactory.dateOf(2020, 1, 31);

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

        // Monthly fixed withdrawal (no percentage, no variation).
        Withdraw withdrawAction = new Withdraw(monthlySpending, 0.0, 0.0, 0.0);

        // Run far enough to include the *next* month-end (2020-02-29) but not rely on an end-date month-end.
        IDate endDate = startDate.plusMonths(2);
        long durationDays = startDate.daysUntil(endDate);

        AtomicInteger monthEndCalls = new AtomicInteger(0);

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
                monthEndCalls.incrementAndGet();
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
        engine.simulatePhases(new LinkedList<>(phases));

        // Assert
        assertEquals(1, monthEndCalls.get(), "Expected exactly one month-end withdrawal (at next month end)");
        assertEquals(initialBalance - monthlySpending, ((ILiveData) withdrawPhase.getLiveData()).getCapital(), 1e-6,
                "Capital should be reduced once, not twice");
    }
}
