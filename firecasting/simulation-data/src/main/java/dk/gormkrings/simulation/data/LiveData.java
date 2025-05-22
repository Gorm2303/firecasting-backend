package dk.gormkrings.simulation.data;

import com.fasterxml.jackson.annotation.JsonValue;
import dk.gormkrings.data.ILiveData;
import dk.gormkrings.simulation.util.Formatter;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class LiveData implements ILiveData {
    private final long startTime;
    private long totalDurationAlive;
    @Setter
    private long sessionDuration;
    @Setter
    private String phaseName;
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
    @Setter
    private double currentNet;
    private double net;

    public LiveData(long startTime) {
        this.startTime = startTime;
    }

    private LiveData(LiveData liveData) {
        this.startTime = liveData.startTime;
        this.totalDurationAlive = liveData.totalDurationAlive;
        this.sessionDuration = liveData.sessionDuration;
        this.phaseName = liveData.phaseName;
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
        this.net = liveData.net;
        this.currentNet = liveData.currentNet;
    }

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
        return "{" +
                phaseName +
                Formatter.formatField("Alive", totalDurationAlive) +
                Formatter.getPrettyDate(this) +
                Formatter.formatField("Capital", capital) +
                Formatter.formatField("Deposited", deposited) +
                Formatter.formatField("Deposit", deposit) +
                Formatter.formatField("Passive", passiveReturned) +
                Formatter.formatField("Returned", returned) +
                Formatter.formatField("Return", currentReturn) +
                Formatter.formatField("Withdrawn", withdrawn) +
                Formatter.formatField("Withdraw", withdraw) +
                Formatter.formatField("Taxed", tax) +
                Formatter.formatField("Tax", currentTax) +
                Formatter.formatField("Inflation", inflation) +
                Formatter.formatField("NetTotal", net) +
                Formatter.formatField("Net", currentNet) +
                "}";
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

    public void subtractFromDeposited(double deposit) {
        this.deposited -= deposit;
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

    public LiveData copy() {
        return new LiveData(this);
    }

    @Override
    public String toCsvRow() {
        Date startdate = new Date((int) startTime);
        Date date = new Date((int) (startTime + totalDurationAlive));
        long day = totalDurationAlive;
        long year = date.getYear() - startdate.getYear();
        long month = date.getMonth() + 12 * year - 1;

        return  phaseName + "," +
                day + "," +
                month + "," +
                year + "," +
                date + "," +
                Formatter.numberToString(capital) + "," +
                Formatter.numberToString(deposited) + "," +
                Formatter.numberToString(deposit) + "," +
                Formatter.numberToString(passiveReturned) + "," +
                Formatter.numberToString(returned) + "," +
                Formatter.numberToString(currentReturn) + "," +
                Formatter.numberToString(withdrawn) + "," +
                Formatter.numberToString(withdraw) + "," +
                Formatter.numberToString(tax) + "," +
                Formatter.numberToString(currentTax) + "," +
                Formatter.numberToString(inflation) + "," +
                Formatter.numberToString(net) + "," +
                Formatter.numberToString(currentNet);
    }

    @JsonValue
    public Map<String, Object> asJson() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("phaseName", phaseName);

        Date startdate = new Date((int) startTime);
        Date date = new Date((int) (startTime + totalDurationAlive));
        long day = totalDurationAlive;
        long year = date.getYear() - startdate.getYear();
        long month = date.getMonth() + 12 * year - 1;

        map.put("day", day);
        map.put("month", month);
        map.put("year", year);
        map.put("date", date.toString());
        map.put("capital", capital);
        map.put("deposited", deposited);
        map.put("deposit", deposit);
        map.put("passiveReturned", passiveReturned);
        map.put("returned", returned);
        map.put("currentReturn", currentReturn);
        map.put("withdrawn", withdrawn);
        map.put("withdraw", withdraw);
        map.put("tax", tax);
        map.put("currentTax", currentTax);
        map.put("inflation", inflation);
        map.put("net", net);
        map.put("currentNet", currentNet);
        return map;
    }

}
