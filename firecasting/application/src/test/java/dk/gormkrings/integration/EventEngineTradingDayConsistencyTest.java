package dk.gormkrings.integration;

import dk.gormkrings.calendar.WeekdayTradingCalendar;
import dk.gormkrings.data.IDate;
import dk.gormkrings.factory.IDateFactory;
import dk.gormkrings.factory.IResultFactory;
import dk.gormkrings.factory.ISnapshotFactory;
import dk.gormkrings.inflation.IInflation;
import dk.gormkrings.phase.IEventPhase;
import dk.gormkrings.phase.eventBased.SimulationEventPhase;
import dk.gormkrings.returns.IReturner;
import dk.gormkrings.simulation.ReturnStep;
import dk.gormkrings.simulation.engine.event.EventEngine;
import dk.gormkrings.simulation.factory.DefaultDateFactory;
import dk.gormkrings.simulation.factory.DefaultResultFactory;
import dk.gormkrings.simulation.factory.DefaultSnapshotFactory;
import dk.gormkrings.simulation.specification.Specification;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.ITaxExemption;
import dk.gormkrings.tax.ITaxRule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EventEngineTradingDayConsistencyTest {

    @AfterEach
    void resetStaticConfig() {
        SimulationEventPhase.configureReturnStep(ReturnStep.DAILY);
        SimulationEventPhase.configureTradingCalendar(new WeekdayTradingCalendar());
    }

    @Test
    void dailyMode_appliesReturnsOnlyOnTradingDays() {
        SimulationEventPhase.configureReturnStep(ReturnStep.DAILY);
        SimulationEventPhase.configureTradingCalendar(new WeekdayTradingCalendar());

        IDateFactory dateFactory = new DefaultDateFactory();
        IResultFactory resultFactory = new DefaultResultFactory();
        ISnapshotFactory snapshotFactory = new DefaultSnapshotFactory();
        EventEngine engine = new EventEngine(dateFactory, resultFactory, snapshotFactory);

        IDate startDate = dateFactory.dateOf(2025, 1, 1);
        long durationDays = 30;

        CountingReturner returner = new CountingReturner();
        ISpecification spec = new Specification(startDate.getEpochDay(), noTax(), returner, noInflation());
        ((dk.gormkrings.data.ILiveData) spec.getLiveData()).addToCapital(100.0);

        IEventPhase phase = new MinimalEventPhase(spec, startDate, durationDays);
        engine.simulatePhases(List.of(phase));

        long expectedTradingDays = countTradingDays(dateFactory, startDate, durationDays, new WeekdayTradingCalendar());
        assertEquals(expectedTradingDays, returner.calls, "daily mode should apply returns only on trading days");
    }

    @Test
    void monthlyMode_doesNotApplyDailyReturns() {
        SimulationEventPhase.configureReturnStep(ReturnStep.MONTHLY);
        SimulationEventPhase.configureTradingCalendar(new WeekdayTradingCalendar());

        IDateFactory dateFactory = new DefaultDateFactory();
        IResultFactory resultFactory = new DefaultResultFactory();
        ISnapshotFactory snapshotFactory = new DefaultSnapshotFactory();
        EventEngine engine = new EventEngine(dateFactory, resultFactory, snapshotFactory);

        // 10 days from the 1st should not include calendar month-end.
        IDate startDate = dateFactory.dateOf(2025, 1, 1);
        long durationDays = 10;

        CountingReturner returner = new CountingReturner();
        ISpecification spec = new Specification(startDate.getEpochDay(), noTax(), returner, noInflation());
        ((dk.gormkrings.data.ILiveData) spec.getLiveData()).addToCapital(100.0);

        IEventPhase phase = new MinimalEventPhase(spec, startDate, durationDays);
        engine.simulatePhases(List.of(phase));

        assertEquals(0, returner.calls, "monthly mode should not apply returns on day events");
    }

    private static long countTradingDays(IDateFactory dateFactory, IDate startDate, long durationDays, WeekdayTradingCalendar calendar) {
        long count = 0;
        // Event/call engines apply day-end logic with sessionDuration starting at 1.
        for (long sessionDay = 1; sessionDay <= durationDays; sessionDay++) {
            IDate d = startDate.plusDays(sessionDay);
            if (calendar.isTradingDay(d)) count++;
        }
        return count;
    }

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

    private static final class CountingReturner implements IReturner {
        long calls;

        @Override
        public double calculateReturn(double amount) {
            calls++;
            return 1.0;
        }

        @Override
        public IReturner copy() {
            return new CountingReturner();
        }
    }

    private static final class MinimalEventPhase extends SimulationEventPhase {
        MinimalEventPhase(ISpecification specification, IDate startDate, long duration) {
            super(specification, startDate, List.<ITaxExemption>of(), duration, "minimal");
        }

        @Override
        public MinimalEventPhase copy(ISpecification specificationCopy) {
            return new MinimalEventPhase(specificationCopy, getStartDate(), getDuration());
        }
    }
}
