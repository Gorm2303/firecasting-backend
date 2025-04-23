package dk.gormkrings.engine.schedule;

import dk.gormkrings.event.EventType;
import dk.gormkrings.event.IEvent;

public interface IScheduleEvent extends IEvent {
    int getEpoch();
    EventType getType();
}
