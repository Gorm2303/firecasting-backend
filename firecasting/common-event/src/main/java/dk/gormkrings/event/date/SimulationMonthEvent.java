package dk.gormkrings.event.date;

import dk.gormkrings.event.SimulationUpdateEvent;
import dk.gormkrings.event.Type;

public interface SimulationMonthEvent extends SimulationUpdateEvent {
    Type getType();
}
