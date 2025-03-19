package dk.gormkrings.event;

import dk.gormkrings.data.LiveData;

public interface SimulationUpdateEvent {
    LiveData getData();
}