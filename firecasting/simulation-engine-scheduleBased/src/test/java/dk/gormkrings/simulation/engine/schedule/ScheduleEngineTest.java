package dk.gormkrings.simulation.engine.schedule;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import dk.gormkrings.factory.IResultFactory;
import dk.gormkrings.factory.ISnapshotFactory;
import dk.gormkrings.engine.schedule.IScheduleFactory;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

public class ScheduleEngineTest {

    @Test
    public void testConstructorInitialization() throws Exception {
        // Create mocks for the dependencies
        IResultFactory resultFactory = mock(IResultFactory.class);
        ISnapshotFactory snapshotFactory = mock(ISnapshotFactory.class);
        IScheduleFactory scheduleFactory = mock(IScheduleFactory.class);

        // Construct the ScheduleEngine instance
        ScheduleEngine engine = new ScheduleEngine(resultFactory, snapshotFactory, scheduleFactory);

        // Verify that the instance fields are correctly assigned
        assertSame(resultFactory, engine.getResultFactory(), "ResultFactory should be assigned correctly");
        assertSame(snapshotFactory, engine.getSnapshotFactory(), "SnapshotFactory should be assigned correctly");

        // Verify that the static scheduleFactory field is correctly assigned using reflection
        Field staticField = ScheduleEngine.class.getDeclaredField("scheduleFactory");
        staticField.setAccessible(true);
        IScheduleFactory actualScheduleFactory = (IScheduleFactory) staticField.get(null); // null for static field
        assertSame(scheduleFactory, actualScheduleFactory, "Static scheduleFactory should be assigned correctly");
    }
}
