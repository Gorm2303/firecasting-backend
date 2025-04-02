package dk.gormkrings.test;

import dk.gormkrings.data.ILiveData;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DummyLiveData implements ILiveData {
    private long totalDurationAlive;
    private long sessionDuration;
    private boolean tradingDay;
    private double deposit;
    private double deposited;
    private double passiveReturn;
    private double passiveReturned;
    private double capital;
    private double inflation;
    private double currentReturn;
    private double returned;
    private double withdraw;
    private double withdrawn;
    private double currentTax;
    private double tax;
    private double currentNet;
    private double net;

    @Override
    public void incrementTime() {
        sessionDuration++;
        totalDurationAlive++;
    }

    @Override
    public void incrementTime(long amount) {
        sessionDuration += amount;
        totalDurationAlive += amount;
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
        return "";
    }

    @Override
    public String toCsvRow() {
        return "";
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

    public void subtractFromReturned(double amount) {
        this.returned -= amount;
    }

    public void addToWithdrawn(double withdraw) {
        this.withdrawn += withdraw;
    }

    public void subtractFromWithdrawn(double amount) {
        this.withdrawn -= amount;
    }

    public void addToPassiveReturned(double passiveReturn) {
        this.passiveReturned += passiveReturn;
    }

    public void subtractFromPassiveReturned(double amount) {
        this.passiveReturned -= amount;
    }

    public void addToTax(double tax) {
        this.tax += tax;
    }

    public void addToInflation(double inflation) {
        this.inflation += inflation;
    }

    public void addToNetEarnings(double netEarnings) {
        this.net += netEarnings;
    }

    @Override
    public long getStartTime() {
        return 0;
    }

    public ILiveData copy() {
        return null;
    }
}
