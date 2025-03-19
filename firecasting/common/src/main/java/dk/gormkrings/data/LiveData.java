package dk.gormkrings.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LiveData implements Live {
    private int currentTimeSpan;
    private double deposit;
    private double capital;
    private float inflation;
    private float rateOfReturn;

    public LiveData() {
        this.currentTimeSpan = 0;
        this.deposit = 0;
        this.capital = 0;
        this.inflation = 0;
        this.rateOfReturn = 0;
    }

    private LiveData(LiveData liveData) {
        this.currentTimeSpan = liveData.currentTimeSpan;
        this.inflation = liveData.inflation;
        this.rateOfReturn = liveData.rateOfReturn;
        this.capital = liveData.capital;
        this.deposit = liveData.deposit;
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

    public void addToCapital(double capital) {
        this.capital += capital;
    }

    public void subtractFromCapital(double capital) {
        this.capital -= capital;
    }

    public void addToDeposit(double deposit) {
        this.deposit += deposit;
    }

    public void subtractFromDeposit(double deposit) {
        this.deposit -= deposit;
    }

    public LiveData copy(LiveData liveData) {
        return new LiveData(liveData);
    }

}
