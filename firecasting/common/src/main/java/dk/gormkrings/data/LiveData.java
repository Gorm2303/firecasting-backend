package dk.gormkrings.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LiveData implements Live {
    private long totalDurationAlive;
    private long sessionDuration;
    private double deposit;
    private double passiveMoney;
    private double capital;
    private float inflation;
    private float rateOfReturn;

    public LiveData() {
        this.sessionDuration = 0;
        this.deposit = 0;
        this.capital = 0;
        this.inflation = 0;
        this.rateOfReturn = 0;
        this.totalDurationAlive = 0;
        this.passiveMoney = 0;
    }

    private LiveData(LiveData liveData) {
        this.sessionDuration = liveData.sessionDuration;
        this.inflation = liveData.inflation;
        this.rateOfReturn = liveData.rateOfReturn;
        this.capital = liveData.capital;
        this.deposit = liveData.deposit;
        this.totalDurationAlive = liveData.totalDurationAlive;
        this.passiveMoney = liveData.passiveMoney;
    }

    @Override
    public void incrementTime() {
        sessionDuration++;
        totalDurationAlive++;
    }

    @Override
    public boolean isLive(long duration) {
        return sessionDuration < duration;
    }

    public void resetSession(){
        sessionDuration = 0;
    }

    @Override
    public String toString() {
        return "LiveData: " +
                "Alive " + totalDurationAlive +
                " - Session " + sessionDuration +
                " - Deposit " + deposit +
                " - Capital " + capital +
                " - Passive " + passiveMoney +
                " - Inflation " + inflation +
                " - RateOfReturn " + rateOfReturn;
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

    public LiveData copy() {
        return new LiveData(this);
    }

}
