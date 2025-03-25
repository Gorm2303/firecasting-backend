package dk.gormkrings.simulation.engine.schedule;

import java.util.List;

public class Schedule {
    private final List<Event> events;

    public Schedule(List<Event> events) {
        this.events = events;
    }

    public void execute() {
        for (Event event : events) {
            event.getAction().run();
        }
    }
}

