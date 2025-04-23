package dk.gormkrings.simulation.engine.schedule;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
        ScheduleEngineAdditionalTest.class,
        ScheduleEngineConcurrentExecutionTest.class,
        ScheduleEngineMultiplePhasesTest.class,
        ScheduleEngineSimulatePhasesTest.class,})
public class ScheduleEngineTest {

}
