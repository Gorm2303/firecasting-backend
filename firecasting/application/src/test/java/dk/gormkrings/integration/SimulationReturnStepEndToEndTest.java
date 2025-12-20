package dk.gormkrings.integration;

import dk.gormkrings.config.SimulationReturnStepInitializer;
import dk.gormkrings.engine.IEngine;
import dk.gormkrings.factory.IDateFactory;
import dk.gormkrings.factory.IResultFactory;
import dk.gormkrings.factory.ISnapshotFactory;
import dk.gormkrings.inflation.IInflation;
import dk.gormkrings.data.ILiveData;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.phase.callBased.SimulationCallPhase;
import dk.gormkrings.returns.IReturner;
import dk.gormkrings.simulation.ReturnStep;
import dk.gormkrings.simulation.engine.call.CallEngine;
import dk.gormkrings.simulation.factory.DefaultDateFactory;
import dk.gormkrings.simulation.factory.DefaultResultFactory;
import dk.gormkrings.simulation.factory.DefaultSnapshotFactory;
import dk.gormkrings.simulation.specification.Specification;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.ITaxExemption;
import dk.gormkrings.tax.ITaxRule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SimulationReturnStepEndToEndTest {

    @AfterEach
    void resetReturnStep() {
        SimulationCallPhase.configureReturnStep(ReturnStep.DAILY);
    }

    @Test
    void minimalSimulation_runsUnderDailyAndMonthly_andCadenceDiffers() {
        SimulationRunStats daily = runWithReturnStep("daily");
        SimulationRunStats monthly = runWithReturnStep("monthly");

        assertTrue(daily.callCount > monthly.callCount,
                "daily mode should apply returns more often than monthly mode");

        assertTrue(monthly.returnPerCall > daily.returnPerCall,
                "per-step return should be larger in monthly mode than daily mode (positive return)");

        assertTrue(daily.totalReturned > 0.0, "daily mode should apply at least one positive return");
        assertTrue(monthly.totalReturned > 0.0, "monthly mode should apply at least one positive return");

        // Over a fixed horizon, totals should be broadly comparable if the per-step return scales with dt.
        double diff = Math.abs(daily.totalReturned - monthly.totalReturned);
        double scale = Math.max(daily.totalReturned, monthly.totalReturned);
        assertTrue(diff / scale < 0.20,
                "total returns should be within 20% between daily and monthly for dt-scaled positive returns");
    }

    private static SimulationRunStats runWithReturnStep(String step) {
        var runner = new ApplicationContextRunner()
                .withUserConfiguration(TestConfig.class)
                .withPropertyValues("simulation.return.step=" + step);

        final SimulationRunStats[] stats = new SimulationRunStats[1];

        runner.run(ctx -> {
            SimulationReturnStepInitializer initializer = ctx.getBean(SimulationReturnStepInitializer.class);
            assertNotNull(initializer);

            CountingDtReturner returner = ctx.getBean(CountingDtReturner.class);
            stats[0] = runMinimalCallEngineSimulation(returner);
        });

        return stats[0];
    }

    private static SimulationRunStats runMinimalCallEngineSimulation(CountingDtReturner returner) {
        IDateFactory dateFactory = new DefaultDateFactory();
        IResultFactory resultFactory = new DefaultResultFactory();
        ISnapshotFactory snapshotFactory = new DefaultSnapshotFactory();
        IEngine engine = new CallEngine(dateFactory, resultFactory, snapshotFactory);

        // 90 days from 2025-01-01 includes 3 month-ends: Jan 31, Feb 28, Mar 31.
        var startDate = dateFactory.dateOf(2025, 1, 1);

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

        ISpecification specification = new Specification(startDate.getEpochDay(), noTax, returner, noInflation);
        ILiveData liveData = (ILiveData) specification.getLiveData();
        liveData.addToCapital(100.0);

        IPhase phase = new SimulationCallPhase(specification, startDate, List.<ITaxExemption>of(), 90, "e2e") {
            @Override
            public IPhase copy(ISpecification specificationCopy) {
                return new SimulationCallPhase(specificationCopy, startDate, List.<ITaxExemption>of(), 90, "e2e") {
                    @Override
                    public IPhase copy(ISpecification specCopy2) {
                        return this;
                    }
                };
            }
        };

        engine.simulatePhases(List.of(phase));
        double totalReturned = liveData.getReturned();
        long callCount = returner.getCalls();
        double perCall = (callCount == 0) ? 0.0 : (totalReturned / callCount);
        return new SimulationRunStats(totalReturned, callCount, perCall);
    }

    static final class SimulationRunStats {
        final double totalReturned;
        final long callCount;
        final double returnPerCall;

        SimulationRunStats(double totalReturned, long callCount, double returnPerCall) {
            this.totalReturned = totalReturned;
            this.callCount = callCount;
            this.returnPerCall = returnPerCall;
        }
    }

    static final class CountingDtReturner implements IReturner {
        private final double annualRate;
        private final double dt;
        private long calls;

        CountingDtReturner(double annualRate, double dt) {
            this.annualRate = annualRate;
            this.dt = dt;
        }

        @Override
        public double calculateReturn(double amount) {
            calls++;
            // Deterministic positive return: amount * annualRate scaled by dt.
            return amount * annualRate * dt;
        }

        long getCalls() {
            return calls;
        }

        @Override
        public IReturner copy() {
            return new CountingDtReturner(annualRate, dt);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class TestConfig {
        @Bean
        SimulationReturnStepInitializer simulationReturnStepInitializer(
                @Value("${simulation.return.step:daily}") String configuredStep
        ) {
            return new SimulationReturnStepInitializer(configuredStep);
        }

        @Bean
        CountingDtReturner countingDtReturner(
                @Value("${simulation.return.step:daily}") String configuredStep
        ) {
            ReturnStep step = ReturnStep.fromProperty(configuredStep);
            // 12% annual rate, scaled by dt; per-step return is larger in monthly mode.
            return new CountingDtReturner(0.12, step.toDt());
        }
    }
}
