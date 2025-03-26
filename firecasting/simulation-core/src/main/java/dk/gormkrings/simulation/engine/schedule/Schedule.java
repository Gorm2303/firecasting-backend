package dk.gormkrings.simulation.engine.schedule;

import lombok.Getter;

import java.util.List;

@Getter
public class Schedule {
    private final List<Event> events;

    public Schedule(List<Event> events) {
        this.events = events;
    }
}
