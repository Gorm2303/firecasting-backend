package dk.gormkrings.simulation.results;

import dk.gormkrings.data.IImmutableData;
import dk.gormkrings.data.ILive;

public final class Snapshot implements IImmutableData {
    private final ILive state;

    public Snapshot(ILive state) {
        this.state = state.copy();
    }

    @Override
    public String toString() {
        return state.toString();
    }
}
