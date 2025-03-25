package dk.gormkrings.simulation.data;

import dk.gormkrings.data.ImmutableData;
import dk.gormkrings.data.Live;
import lombok.Getter;
import lombok.Setter;

public final class Snapshot implements ImmutableData {
    private final Live state;

    public Snapshot(Live state) {
        this.state = state.copy();
    }

    @Override
    public String toString() {
        return state.toString();
    }
}
