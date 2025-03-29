package dk.gormkrings.engine.schedule;

import java.util.List;

public interface ISchedule {
    List<IScheduleEvent> getEvents();
}
