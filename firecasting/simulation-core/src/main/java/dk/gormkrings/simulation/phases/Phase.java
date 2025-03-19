package dk.gormkrings.simulation.phases;

import dk.gormkrings.data.LiveData;

public interface Phase {
    LiveData getLiveData();
    String getName();
}
