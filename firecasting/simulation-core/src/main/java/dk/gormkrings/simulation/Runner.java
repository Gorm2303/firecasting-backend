package dk.gormkrings.simulation;

import dk.gormkrings.data.LiveData;
import dk.gormkrings.simulation.data.Result;

public interface Runner {
    Result run(LiveData liveData);
}
