package dk.gormkrings.event;

public interface SimulationMonthEvent extends SimulationUpdateEvent {
    Type getType();
}
