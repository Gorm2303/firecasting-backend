package dk.gormkrings.simulation.engine.schedule;

import lombok.Getter;

@Getter
public class Event {
    private final int epochDay;
    private final Runnable action;

    public Event(int epochDay, Runnable action) {
        this.epochDay = epochDay;
        this.action = action;
    }

}

