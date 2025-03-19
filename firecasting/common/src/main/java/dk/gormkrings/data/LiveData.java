package dk.gormkrings.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LiveData implements Live {
    private int currentTimeSpan;
    private double capital;
    private float inflation;
    private float rateOfReturn;

    public LiveData() {
        this.currentTimeSpan = 0;
        this.capital = 0;
        this.inflation = 0;
        this.rateOfReturn = 0;
    }

    private LiveData(LiveData liveData) {
        this.currentTimeSpan = liveData.currentTimeSpan;
        this.inflation = liveData.inflation;
        this.rateOfReturn = liveData.rateOfReturn;
        this.capital = liveData.capital;
    }

    @Override
    public void incrementTime() {
        currentTimeSpan++;
    }

    @Override
    public boolean isLive(long duration) {
        return currentTimeSpan < duration;
    }

    @Override
    public String toString() {
        return "LiveData {" +
                "timeSpan=" + currentTimeSpan +
                ", capital=" + capital +
                ", inflation=" + inflation +
                ", rateOfReturn=" + rateOfReturn +
                '}';
    }

    public LiveData copy(LiveData liveData) {
        return new LiveData(liveData);
    }

}
