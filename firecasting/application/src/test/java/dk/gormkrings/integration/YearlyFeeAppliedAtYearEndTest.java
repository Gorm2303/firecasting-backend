package dk.gormkrings.integration;

import dk.gormkrings.action.Deposit;
import dk.gormkrings.action.Passive;
import dk.gormkrings.calendar.WeekdayTradingCalendar;
import dk.gormkrings.data.IDate;
import dk.gormkrings.data.ILiveData;
import dk.gormkrings.factory.IDateFactory;
import dk.gormkrings.factory.IResultFactory;
import dk.gormkrings.factory.ISnapshotFactory;
import dk.gormkrings.fee.DefaultYearlyFee;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class YearlyFeeAppliedAtYearEndTest {

    @Test
    void yearlyFee_isDeductedFromCapital_atYearEnd() {
        // Arrange
        final double initialBalance = 100_000.0;
        final double yearlyFeePct = 1.0; // 1% per year

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
        returner.setAveragePercentage(0.0f);

        ISpecification specification = new Specification(
                startDate.getEpochDay(),
                noTax,
                returner,
                new DefaultInflation(1.0),
                new DefaultYearlyFee(yearlyFeePct)
        );

        DepositCallPhase initialDeposit = new DepositCallPhase(
                specification,
                startDate,
                List.<ITaxExemption>of(),
                0L,
                new Deposit(initialBalance, 0.0, 0.0),
                ReturnStep.MONTHLY,
                new WeekdayTradingCalendar()
        );

        IDate endDate = startDate.plusMonths(12);
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
        assertTrue(snapshots.size() >= 2, "Expected at least a start snapshot and a year-end snapshot");

        ILiveData first = snapshots.getFirst().getState();
        assertEquals(initialBalance, first.getCapital(), 1e-9, "Initial deposit should be applied at start");

        ILiveData last = snapshots.getLast().getState();
        assertEquals(initialBalance * (1.0 - yearlyFeePct / 100.0), last.getCapital(), 1e-9,
                "Yearly fee should be deducted from capital at year-end");
    }
}
