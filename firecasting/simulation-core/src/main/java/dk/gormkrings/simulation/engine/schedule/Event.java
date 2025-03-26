package dk.gormkrings.simulation.engine.schedule;

import lombok.Getter;

@Getter
public class Event {
    private final int epochDay;
    private final EventType type;

    public Event(int epochDay, EventType type) {
        this.epochDay = epochDay;
        this.type = type;
    }
}
