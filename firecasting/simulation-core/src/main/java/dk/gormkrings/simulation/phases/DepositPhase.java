package dk.gormkrings.simulation.phases;

import dk.gormkrings.data.LiveData;

public class DepositPhase extends SimulationPhase {
    public DepositPhase(LiveData liveData) {
        super(liveData);
        setName("Deposit Phase");
    }

}
