package dk.gormkrings.simulation.data;

import dk.gormkrings.data.ImmutableData;
import dk.gormkrings.data.LiveData;
import lombok.Getter;
import lombok.Setter;

public final class Snapshot implements ImmutableData {
    private final LiveData state;
    @Setter
    @Getter
    private String extraInfo;

    public Snapshot(LiveData state) {
        this.state = state.copy();
    }

    public long getCurrentTimeSpan() {
        return state.getSessionDuration();
    }

    public double getCapital() {
        return state.getCapital();
    }

    public float getInflation() {
        return state.getInflation();
    }

    public float getRateOfReturn() {
        return state.getRateOfReturn();
    }

    @Override
    public String toString() {
        return state.toString();
    }
}
