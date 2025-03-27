package dk.gormkrings.simulation.result;

import dk.gormkrings.data.ILiveData;
import dk.gormkrings.result.ISnapshot;

public final class Snapshot implements ISnapshot {
    private final ILiveData state;

    public Snapshot(ILiveData state) {
        this.state = state.copy();
    }

    @Override
    public String toString() {
        return state.toString();
    }

    public String toCsvRow() {
        return state.toCsvRow();
    }
}
