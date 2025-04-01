package dk.gormkrings.simulation.engine.schedule;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
        DefaultScheduleFactoryInitializationTest.class,
        DefaultScheduleFactoryBoundaryTest.class,
        DefaultScheduleFactoryEventGenerationTest.class,
})public class DefaultScheduleFactoryTest {

}