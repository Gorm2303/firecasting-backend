package dk.gormkrings.data;

import lombok.Getter;
import lombok.Setter;

import static dk.gormkrings.util.Util.formatField;

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
    private double currentTax;
    private double tax;
    private double netEarnings;

    public LiveData() {
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
        this.currentTax = liveData.currentTax;
        this.tax = liveData.tax;
        this.netEarnings = liveData.netEarnings;
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
        return "LiveData:" +
                getAliveInfo() +
                getSessionInfo() +
                getDepositInfo() +
                getDepositedInfo() +
                getPassiveInfo() +
                getCapitalInfo() +
                getInflationInfo() +
                getReturnInfo() +
                getReturnedInfo() +
                getWithdrawInfo() +
                getWithdrawnInfo() +
                getTaxInfo() +
                getTaxedInfo() +
                getEarningsInfo();
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
        this.netEarnings += netEarnings;
    }

    public LiveData copy() {
        return new LiveData(this);
    }

    public String getAliveInfo() {
        return formatField("Alive", totalDurationAlive);
    }

    public String getSessionInfo() {
        return formatField("Session", sessionDuration);
    }

    public String getDepositInfo() {
        return formatField("Deposit", deposit);
    }

    public String getDepositedInfo() {
        return formatField("Deposited", deposited);
    }

    public String getPassiveInfo() {
        return formatField("Passive", passiveReturned);
    }

    public String getCapitalInfo() {
        return formatField("Capital", capital);
    }

    public String getInflationInfo() {
        return formatField("Inflation", inflation);
    }

    public String getReturnInfo() {
        return formatField("Return", currentReturn);
    }

    public String getReturnedInfo() {
        return formatField("Returned", returned);
    }

    public String getWithdrawInfo() {
        return formatField("Withdraw", withdraw);
    }

    public String getWithdrawnInfo() {
        return formatField("Withdrawn", withdrawn);
    }

    public String getTaxInfo() {
        return formatField("Tax", currentTax);
    }

    public String getTaxedInfo() {
        return formatField("Taxed", tax);
    }

    public String getEarningsInfo() {
        return formatField("Earnings", netEarnings);
    }


}
