package dk.gormkrings.event;

public interface SimulationYearEvent extends SimulationUpdateEvent {
    Type getType();
}
