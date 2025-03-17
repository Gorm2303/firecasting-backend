package dk.gormkrings.simulation;

public interface SimulationUpdateEvent {
    int getDay();
    LiveData getData();
}