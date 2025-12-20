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
        double dailyReturned = runWithReturnStep("daily");
        double monthlyReturned = runWithReturnStep("monthly");

        assertTrue(monthlyReturned > 0.0, "monthly mode should apply at least one return");
        assertTrue(dailyReturned > monthlyReturned * 10.0,
                "daily mode should apply many more returns than monthly mode");
    }

    private static double runWithReturnStep(String step) {
        var runner = new ApplicationContextRunner()
                .withUserConfiguration(TestConfig.class)
                .withPropertyValues("simulation.return.step=" + step);

        final double[] returned = new double[1];

        runner.run(ctx -> {
            SimulationReturnStepInitializer initializer = ctx.getBean(SimulationReturnStepInitializer.class);
            assertNotNull(initializer);
            returned[0] = runMinimalCallEngineSimulation();
        });

        return returned[0];
    }

    private static double runMinimalCallEngineSimulation() {
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

        IReturner constantReturn = new IReturner() {
            @Override
            public double calculateReturn(double amount) {
                return 1.0;
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

        ISpecification specification = new Specification(startDate.getEpochDay(), noTax, constantReturn, noInflation);
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
        return liveData.getReturned();
    }

    @Configuration(proxyBeanMethods = false)
    static class TestConfig {
        @Bean
        SimulationReturnStepInitializer simulationReturnStepInitializer(
                @Value("${simulation.return.step:daily}") String configuredStep
        ) {
            return new SimulationReturnStepInitializer(configuredStep);
        }
    }
}
