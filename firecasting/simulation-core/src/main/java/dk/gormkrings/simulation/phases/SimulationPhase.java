package dk.gormkrings.simulation.phases;

import dk.gormkrings.data.LiveData;
import lombok.Getter;
import lombok.Setter;

@Getter
public abstract class SimulationPhase implements Phase {
    private final LiveData liveData;
    @Setter
    private String name;

    public SimulationPhase(LiveData liveData) {
        this.liveData = liveData;
    }
}
