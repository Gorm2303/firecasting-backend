package dk.gormkrings.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LiveData implements Live {
    private long totalDurationAlive;
    private long sessionDuration;
    private double deposit;
    private double deposited;
    private double passiveMoney;
    private double capital;
    private float inflation;
    private double currentReturn;
    private double returned;
    private double withdraw;
    private double withdrawn;

    public LiveData() {
        this.totalDurationAlive = 0;
        this.sessionDuration = 0;
        this.deposit = 0;
        this.deposited = 0;
        this.passiveMoney = 0;
        this.capital = 0;
        this.inflation = 0;
        this.currentReturn = 0;
        this.returned = 0;
        this.withdraw = 0;
        this.withdrawn = 0;
    }

    private LiveData(LiveData liveData) {
        this.totalDurationAlive = liveData.totalDurationAlive;
        this.sessionDuration = liveData.sessionDuration;
        this.deposit = liveData.deposit;
        this.deposited = liveData.deposited;
        this.passiveMoney = liveData.passiveMoney;
        this.capital = liveData.capital;
        this.inflation = liveData.inflation;
        this.currentReturn = liveData.currentReturn;
        this.returned = liveData.returned;
        this.withdraw = liveData.withdraw;
        this.withdrawn = liveData.withdrawn;
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
                " - Deposited " + deposited +
                " - Capital " + capital +
                " - Passive " + passiveMoney +
                " - Inflation " + inflation +
                " - Returned " + returned;
    }

    public void addToCapital(double capital) {
        this.capital += capital;
    }

    public void subtractFromCapital(double capital) {
        this.capital -= capital;
    }

    public void addToDeposit(double deposit) {
        this.deposited += deposit;
    }

    public void subtractFromDeposit(double deposit) {
        this.deposited -= deposit;
    }

    public void addToReturned(double returned) {
        this.returned += returned;
    }

    public void subtractFromReturned(double returned) {
        this.returned -= returned;
    }

    public void addToWithdrawn(double withdraw) {
        this.withdrawn += withdraw;
    }

    public LiveData copy() {
        return new LiveData(this);
    }

}
