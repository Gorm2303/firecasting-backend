package dk.gormkrings.simulation.data;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
        DateConstructorTest.class,
        DateArithmeticTest.class,
        DateGettersTest.class,
        DateBoundaryTest.class,
        DateOtherTest.class,
})
public class DateTestSuite {
}
