package dk.gormkrings.integration;

import dk.gormkrings.action.Deposit;
import dk.gormkrings.action.Passive;
import dk.gormkrings.action.Withdraw;
import dk.gormkrings.calendar.WeekdayTradingCalendar;
import dk.gormkrings.data.IDate;
import dk.gormkrings.data.ILiveData;
import dk.gormkrings.factory.IDateFactory;
import dk.gormkrings.factory.IResultFactory;
import dk.gormkrings.factory.ISnapshotFactory;
import dk.gormkrings.inflation.IInflation;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.phase.callBased.DepositCallPhase;
import dk.gormkrings.phase.callBased.PassiveCallPhase;
import dk.gormkrings.phase.callBased.WithdrawCallPhase;
import dk.gormkrings.returns.IReturner;
import dk.gormkrings.simulation.ReturnStep;
import dk.gormkrings.simulation.data.Date;
import dk.gormkrings.simulation.data.LiveData;
import dk.gormkrings.simulation.engine.schedule.DefaultScheduleFactory;
import dk.gormkrings.simulation.engine.schedule.ScheduleEngine;
import dk.gormkrings.simulation.factory.DefaultDateFactory;
import dk.gormkrings.simulation.factory.DefaultResultFactory;
import dk.gormkrings.simulation.factory.DefaultSnapshotFactory;
import dk.gormkrings.simulation.result.Snapshot;
import dk.gormkrings.simulation.specification.Specification;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.ITaxExemption;
import dk.gormkrings.tax.ITaxRule;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StartDateAndShortPhaseIntegrationTest {

    private static ITaxRule noTax() {
        return new ITaxRule() {
            @Override
            public double calculateTax(double amount) {
                return 0.0;
            }

            @Override
            public ITaxRule copy() {
                return this;
            }
        };
    }

    private static IReturner zeroReturner() {
        return new IReturner() {
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
    }

    private static IInflation noInflation() {
        return new IInflation() {
            @Override
            public double calculateInflation() {
                return 1.0;
            }

            @Override
            public IInflation copy() {
                return this;
            }
        };
    }

    private static ScheduleEngine newEngine(IDateFactory dateFactory) {
        IResultFactory resultFactory = new DefaultResultFactory();
        ISnapshotFactory snapshotFactory = new DefaultSnapshotFactory();
        var scheduleFactory = new DefaultScheduleFactory(dateFactory);
        return new ScheduleEngine(resultFactory, snapshotFactory, scheduleFactory);
    }

    @Test
    void startDateIsUsedAndYearlyIncreaseStillWorks_midYearStart() {
        // Arrange
        IDateFactory dateFactory = new DefaultDateFactory();
        ScheduleEngine engine = newEngine(dateFactory);

        // Mid-year start date.
        IDate startDate = dateFactory.dateOf(2027, 6, 18);
        ISpecification spec = new Specification(startDate.getEpochDay(), noTax(), zeroReturner(), noInflation());

        // 8 months: includes Dec 31 year-end, so yearly increase should affect the *next* month.
        IDate endDate = startDate.plusMonths(8);
        long durationDays = startDate.daysUntil(endDate);

        List<Double> monthEndDeposits = new ArrayList<>();

        IPhase depositPhase = new DepositCallPhase(
                spec,
                startDate,
                List.<ITaxExemption>of(),
                durationDays,
                new Deposit(1_000.0, 100.0, 100.0), // +100% yearly
                ReturnStep.MONTHLY,
                new WeekdayTradingCalendar()
        ) {
            @Override
            public void onMonthEnd() {
                super.onMonthEnd();
                monthEndDeposits.add(((LiveData) getLiveData()).getDeposit());
            }

            @Override
            public DepositCallPhase copy(ISpecification specificationCopy) {
                return this; // single-threaded deterministic test
            }
        };

        engine.init(new LinkedList<>(List.of(depositPhase)));

        // Act
        var result = engine.simulatePhases(new LinkedList<>(List.of(depositPhase)));

        // Assert: start date anchors time.
        assertFalse(result.getSnapshots().isEmpty());
        var first = (Snapshot) result.getSnapshots().get(0);
        ILiveData state0 = first.getState();
        Date d0 = new Date((int) (state0.getStartTime() + state0.getTotalDurationAlive()));
        assertEquals("2027-06-18", d0.toString(), "First snapshot should reflect the chosen startDate");

        // Assert: monthly deposits happen, and yearly increase takes effect after year-end.
        assertTrue(monthEndDeposits.size() >= 2, "Expected multiple month-end deposits");
        // Last month in the 8-month window is January 2028 month-end; it should reflect the +100% yearly increase.
        assertEquals(200.0, monthEndDeposits.get(monthEndDeposits.size() - 1), 1e-6, "Monthly deposit should be doubled after year-end");
        // Earlier month-ends should be the original 100.
        assertEquals(100.0, monthEndDeposits.get(0), 1e-6);
    }

    @Test
    void phaseUnderOneYear_isValidAndRuns_sixMonthsDeposit() {
        IDateFactory dateFactory = new DefaultDateFactory();
        ScheduleEngine engine = newEngine(dateFactory);

        IDate startDate = dateFactory.dateOf(2027, 6, 18);
        ISpecification spec = new Specification(startDate.getEpochDay(), noTax(), zeroReturner(), noInflation());

        long durationDays = startDate.daysUntil(startDate.plusMonths(6));
        assertTrue(durationDays > 0);

        IPhase depositPhase = new DepositCallPhase(
                spec,
                startDate,
                List.<ITaxExemption>of(),
                durationDays,
                new Deposit(500.0, 0.0, 0.0),
                ReturnStep.MONTHLY,
                new WeekdayTradingCalendar()
        );

        engine.init(new LinkedList<>(List.of(depositPhase)));
        var result = engine.simulatePhases(new LinkedList<>(List.of(depositPhase)));
        assertFalse(result.getSnapshots().isEmpty());
        assertEquals(500.0, ((ILiveData) depositPhase.getLiveData()).getCapital(), 1e-6);
    }

    @Test
    void shortPhaseSequence_2mDeposit_3mPassive_4mWithdraw_runsAsExpected() {
        IDateFactory dateFactory = new DefaultDateFactory();
        ScheduleEngine engine = newEngine(dateFactory);

        IDate start = dateFactory.dateOf(2027, 6, 18);
        ISpecification spec = new Specification(start.getEpochDay(), noTax(), zeroReturner(), noInflation());

        IDate depositEnd = start.plusMonths(2);
        IDate passiveEnd = depositEnd.plusMonths(3);
        IDate withdrawEnd = passiveEnd.plusMonths(4);

        long depositDays = start.daysUntil(depositEnd);
        long passiveDays = depositEnd.daysUntil(passiveEnd);
        long withdrawDays = passiveEnd.daysUntil(withdrawEnd);

        IPhase deposit = new DepositCallPhase(
                spec,
                start,
                List.<ITaxExemption>of(),
                depositDays,
                new Deposit(1_000.0, 0.0, 0.0),
                ReturnStep.MONTHLY,
                new WeekdayTradingCalendar()
        );

        IPhase passive = new PassiveCallPhase(
                spec,
                depositEnd,
                List.<ITaxExemption>of(),
                passiveDays,
                new Passive(),
                ReturnStep.MONTHLY,
                new WeekdayTradingCalendar()
        );

        // Withdraw 300/month for 4 month-ends; last withdraw should clamp to remaining capital.
        IPhase withdraw = new WithdrawCallPhase(
                spec,
                passiveEnd,
                List.<ITaxExemption>of(),
                withdrawDays,
                new Withdraw(300.0, 0.0, 0.0, 0.0),
                ReturnStep.MONTHLY,
                new WeekdayTradingCalendar()
        );

        List<IPhase> phases = new LinkedList<>(List.of(deposit, passive, withdraw));
        engine.init(new LinkedList<>(phases));
        engine.simulatePhases(new LinkedList<>(phases));

        assertEquals(0.0, ((ILiveData) withdraw.getLiveData()).getCapital(), 1e-6, "Should be fully withdrawn by end of withdraw phase");
        assertEquals(1_000.0, ((ILiveData) withdraw.getLiveData()).getWithdrawn(), 1e-6, "Total withdrawn should equal initial capital in this setup");
    }

    @Test
    void depositPhase_duration0Or1Month_canBeInitialDepositOnly() {
        IDateFactory dateFactory = new DefaultDateFactory();
        IDate start = dateFactory.dateOf(2027, 6, 18);

        // 0 months
        {
            ScheduleEngine engine = newEngine(dateFactory);
            ISpecification spec = new Specification(start.getEpochDay(), noTax(), zeroReturner(), noInflation());
            long days = start.daysUntil(start.plusMonths(0));
            IPhase deposit0 = new DepositCallPhase(
                    spec,
                    start,
                    List.<ITaxExemption>of(),
                    days,
                    new Deposit(5_000.0, 0.0, 0.0),
                    ReturnStep.MONTHLY,
                    new WeekdayTradingCalendar()
            );

            engine.init(new LinkedList<>(List.of(deposit0)));
            engine.simulatePhases(new LinkedList<>(List.of(deposit0)));
            assertEquals(5_000.0, ((ILiveData) deposit0.getLiveData()).getDeposited(), 1e-6);
            assertEquals(5_000.0, ((ILiveData) deposit0.getLiveData()).getCapital(), 1e-6);
        }

        // 1 month
        {
            ScheduleEngine engine = newEngine(dateFactory);
            ISpecification spec = new Specification(start.getEpochDay(), noTax(), zeroReturner(), noInflation());
            long days = start.daysUntil(start.plusMonths(1));
            IPhase deposit1 = new DepositCallPhase(
                    spec,
                    start,
                    List.<ITaxExemption>of(),
                    days,
                    new Deposit(5_000.0, 0.0, 0.0),
                    ReturnStep.MONTHLY,
                    new WeekdayTradingCalendar()
            );

            engine.init(new LinkedList<>(List.of(deposit1)));
            engine.simulatePhases(new LinkedList<>(List.of(deposit1)));
            assertEquals(5_000.0, ((ILiveData) deposit1.getLiveData()).getDeposited(), 1e-6);
            assertEquals(5_000.0, ((ILiveData) deposit1.getLiveData()).getCapital(), 1e-6);
        }
    }

    @Test
    void withdrawPhase_duration0Or1Month_canWithdrawAllAtOnce() {
        IDateFactory dateFactory = new DefaultDateFactory();
        IDate start = dateFactory.dateOf(2027, 6, 18);

        // 0 months: should withdraw immediately at phase start.
        {
            ScheduleEngine engine = newEngine(dateFactory);
            ISpecification spec = new Specification(start.getEpochDay(), noTax(), zeroReturner(), noInflation());
            long days = start.daysUntil(start.plusMonths(0));

            // Seed capital
            ILiveData data = (ILiveData) spec.getLiveData();
            data.addToCapital(1_000.0);
            data.addToDeposited(1_000.0);

            IPhase w0 = new WithdrawCallPhase(
                    spec,
                    start,
                    List.<ITaxExemption>of(),
                    days,
                    new Withdraw(9_999.0, 0.0, 0.0, 0.0),
                    ReturnStep.MONTHLY,
                    new WeekdayTradingCalendar()
            );

            engine.init(new LinkedList<>(List.of(w0)));
            engine.simulatePhases(new LinkedList<>(List.of(w0)));

            assertEquals(0.0, ((ILiveData) w0.getLiveData()).getCapital(), 1e-6);
            assertEquals(1_000.0, ((ILiveData) w0.getLiveData()).getWithdrawn(), 1e-6);
        }

        // 1 month: should withdraw once at month end.
        {
            ScheduleEngine engine = newEngine(dateFactory);
            ISpecification spec2 = new Specification(start.getEpochDay(), noTax(), zeroReturner(), noInflation());
            ILiveData data2 = (ILiveData) spec2.getLiveData();
            data2.addToCapital(1_000.0);
            data2.addToDeposited(1_000.0);

            long days = start.daysUntil(start.plusMonths(1));
            IPhase w1 = new WithdrawCallPhase(
                    spec2,
                    start,
                    List.<ITaxExemption>of(),
                    days,
                    new Withdraw(9_999.0, 0.0, 0.0, 0.0),
                    ReturnStep.MONTHLY,
                    new WeekdayTradingCalendar()
            );

            engine.init(new LinkedList<>(List.of(w1)));
            engine.simulatePhases(new LinkedList<>(List.of(w1)));

            assertEquals(0.0, ((ILiveData) w1.getLiveData()).getCapital(), 1e-6);
            assertEquals(1_000.0, ((ILiveData) w1.getLiveData()).getWithdrawn(), 1e-6);
        }
    }
}
