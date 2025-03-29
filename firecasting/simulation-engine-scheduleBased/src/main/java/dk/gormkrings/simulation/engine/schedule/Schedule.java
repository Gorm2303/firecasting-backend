package dk.gormkrings.simulation.engine.schedule;

import dk.gormkrings.engine.schedule.ISchedule;
import dk.gormkrings.engine.schedule.IScheduleEvent;
import lombok.Getter;

import java.util.List;

@Getter
public class Schedule implements ISchedule {
    private final List<IScheduleEvent> events;

    public Schedule(List<IScheduleEvent> events) {
        this.events = events;
    }
}
