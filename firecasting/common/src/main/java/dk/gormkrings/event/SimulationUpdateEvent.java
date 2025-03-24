package dk.gormkrings.event;

import dk.gormkrings.data.Live;

public interface SimulationUpdateEvent {
    Live getData();
}