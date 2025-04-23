package dk.gormkrings.simulation.engine.schedule;

import dk.gormkrings.engine.schedule.IScheduleEvent;
import dk.gormkrings.event.EventType;
import lombok.Getter;

@Getter
public class ScheduleEvent implements IScheduleEvent {
    private final int epoch;
    private final EventType type;

    public ScheduleEvent(int epoch, EventType type) {
        this.epoch = epoch;
        this.type = type;
    }
}
