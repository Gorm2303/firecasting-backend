package dk.gormkrings.simulation.engine.schedule;

import dk.gormkrings.event.IEvent;
import lombok.Getter;

import java.util.List;

@Getter
public class Schedule {
    private final List<IEvent> events;

    public Schedule(List<IEvent> events) {
        this.events = events;
    }
}
