package dk.gormkrings.data;

import lombok.Getter;
import lombok.Setter;

@Getter
public class LiveData implements Live {
    private long totalDurationAlive;
    @Setter
    private long sessionDuration;
    @Setter
    private double deposit;
    private double deposited;
    @Setter
    private double passiveReturn;
    private double passiveReturned;
    private double capital;
    private double inflation;
    @Setter
    private double currentReturn;
    private double returned;
    @Setter
    private double withdraw;
    private double withdrawn;
    @Setter
    private boolean debug = false;

    public LiveData() {
        this.totalDurationAlive = 0;
        this.sessionDuration = 0;
        this.deposit = 0;
        this.deposited = 0;
        this.passiveReturned = 0;
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
        this.passiveReturn = liveData.passiveReturn;
        this.passiveReturned = liveData.passiveReturned;
        this.capital = liveData.capital;
        this.inflation = liveData.inflation;
        this.currentReturn = liveData.currentReturn;
        this.returned = liveData.returned;
        this.withdraw = liveData.withdraw;
        this.withdrawn = liveData.withdrawn;
        this.debug = liveData.debug;
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
                " - Deposited " + deposited +
                " - Passive " + passiveReturned +
                " - Capital " + capital +
                " - Inflation " + inflation +
                " - Return " + currentReturn +
                " - Returned " + returned +
                " - withdraw " + withdraw +
                " - withdrawn " + withdrawn;
    }

    public void addToCapital(double capital) {
        this.capital += capital;
    }

    public void subtractFromCapital(double capital) {
        this.capital -= capital;
    }

    public void addToDeposited(double deposit) {
        this.deposited += deposit;
    }

    public void addToReturned(double returned) {
        this.returned += returned;
    }

    public void addToWithdrawn(double withdraw) {
        this.withdrawn += withdraw;
    }

    public void addToPassiveReturned(double passiveReturn) {
        this.passiveReturned += passiveReturn;
    }

    public void addToInflation(double inflation) {
        this.inflation += inflation;
    }

    public LiveData copy() {
        return new LiveData(this);
    }

}
