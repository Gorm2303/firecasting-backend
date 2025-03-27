package dk.gormkrings.simulation.event;

import dk.gormkrings.event.EventType;
import dk.gormkrings.event.IEvent;
import lombok.Getter;

@Getter
public class Event implements IEvent {
    private final int epochDay;
    private final EventType type;

    public Event(int epochDay, EventType type) {
        this.epochDay = epochDay;
        this.type = type;
    }
}
