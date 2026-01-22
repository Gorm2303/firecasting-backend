package dk.gormkrings.integration;

import dk.gormkrings.action.Deposit;
import dk.gormkrings.action.Passive;
import dk.gormkrings.calendar.WeekdayTradingCalendar;
import dk.gormkrings.data.IDate;
import dk.gormkrings.factory.IDateFactory;
import dk.gormkrings.factory.IResultFactory;
import dk.gormkrings.factory.ISnapshotFactory;
import dk.gormkrings.inflation.DefaultInflation;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.phase.callBased.DepositCallPhase;
import dk.gormkrings.phase.callBased.PassiveCallPhase;
import dk.gormkrings.result.ISnapshot;
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

class ZeroDurationDepositPhaseInitialDepositSimulationTest {

    @Test
    void zeroMonthDepositPhase_allowsInitialDeposit_andSimulationRuns() {
        // Arrange: a user wants to deposit a large amount immediately (0 months / 0 years phase duration).
        final double initialDepositAmount = 1_000_000.0;

        IDateFactory dateFactory = new DefaultDateFactory();
        IResultFactory resultFactory = new DefaultResultFactory();
        ISnapshotFactory snapshotFactory = new DefaultSnapshotFactory();

        var scheduleFactory = new DefaultScheduleFactory(dateFactory);
        var engine = new ScheduleEngine(resultFactory, snapshotFactory, scheduleFactory);

        IDate startDate = dateFactory.dateOf(2025, 1, 1);

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

        IReturner zeroReturn = new IReturner() {
            @Override
            public double calculateReturn(double amount) {
                return 0.0;
            }

            @Override
            public IReturner copy() {
                return this;
            }
        };

        ISpecification specification = new Specification(
                startDate.getEpochDay(),
                noTax,
                zeroReturn,
                new DefaultInflation(1.0)
        );

        // This mirrors the application behavior: duration in months converted to days via date arithmetic.
        int depositDurationInMonths = 0;
        long depositDays = startDate.daysUntil(startDate.plusMonths(depositDurationInMonths));
        assertEquals(0L, depositDays, "0 months should convert to 0 days");

        // Next phase starts immediately (same date) when duration is 0 months.
        IDate passiveStartDate = startDate.plusMonths(depositDurationInMonths);
        long passiveDays = passiveStartDate.daysUntil(passiveStartDate.plusMonths(12));
        assertTrue(passiveDays > 0, "Passive phase should span at least one day");

        DepositCallPhase depositPhase = new DepositCallPhase(
                specification,
                startDate,
                List.<ITaxExemption>of(),
                depositDays,
                new Deposit(initialDepositAmount, 0.0, 0.0),
                ReturnStep.DAILY,
                new WeekdayTradingCalendar()
        );

        PassiveCallPhase passivePhase = new PassiveCallPhase(
                specification,
                passiveStartDate,
                List.<ITaxExemption>of(),
                passiveDays,
                new Passive(),
                ReturnStep.DAILY,
                new WeekdayTradingCalendar()
        );

        List<IPhase> phases = List.of(depositPhase, passivePhase);

        // Act
        engine.init(phases);
        var result = engine.simulatePhases(new LinkedList<>(phases));

        // Assert: simulation produces at least the start snapshot, and it includes the initial deposit.
        List<ISnapshot> snapshots = result.getSnapshots();
        assertNotNull(snapshots);
        assertTrue(snapshots.size() >= 2, "Expected start snapshot + a year-end snapshot from the passive year");

        double startCapital = snapshots.getFirst().getState().getCapital();
        assertEquals(initialDepositAmount, startCapital, 1e-9,
                "Start snapshot should include initial deposit even when deposit phase has 0 duration");

        double endCapital = snapshots.getLast().getState().getCapital();
        assertEquals(initialDepositAmount, endCapital, 1e-9,
                "With zero returns and no further cashflows, capital should remain equal to initial deposit");
    }
}
