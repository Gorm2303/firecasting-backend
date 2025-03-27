package dk.gormkrings.simulation.results;

import dk.gormkrings.data.IImmutableData;
import dk.gormkrings.data.ILiveData;

public final class Snapshot implements IImmutableData {
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
