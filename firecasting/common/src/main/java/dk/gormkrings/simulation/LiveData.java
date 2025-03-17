package dk.gormkrings.simulation;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LiveData {
    private final int duration;
    private int currentTimeSpan;
    private double capital;
    private float inflation;
    private float rateOfReturn;

    public LiveData(int duration, double capital) {
        this.duration = duration;
        this.capital = capital;
    }
}
