package dk.gormkrings.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LiveData implements Live {
    private int currentTimeSpan;
    private final int duration;
    private double capital;
    private float inflation;
    private float rateOfReturn;

    public LiveData(int duration, double capital) {
        this.currentTimeSpan = 0;
        this.duration = duration;
        this.capital = capital;
        this.inflation = 0;
        this.rateOfReturn = 0;
    }

    private LiveData(LiveData liveData) {
        this.duration = liveData.duration;
        this.currentTimeSpan = liveData.currentTimeSpan;
        this.inflation = liveData.inflation;
        this.rateOfReturn = liveData.rateOfReturn;
        this.capital = liveData.capital;
    }

    @Override
    public void incrementTime() {
        if (currentTimeSpan < duration) {
            currentTimeSpan++;
        }
    }

    @Override
    public boolean isLive() {
        return currentTimeSpan < duration;
    }

    @Override
    public String toString() {
        return "LiveData {" +
                "timeSpan=" + currentTimeSpan +
                "/" + duration +
                ", capital=" + capital +
                ", inflation=" + inflation +
                ", rateOfReturn=" + rateOfReturn +
                '}';
    }

    public LiveData copy(LiveData liveData) {
        return new LiveData(liveData);
    }

}
