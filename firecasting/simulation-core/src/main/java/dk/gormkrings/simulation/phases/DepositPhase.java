package dk.gormkrings.simulation.phases;

import dk.gormkrings.Deposit;
import dk.gormkrings.data.LiveData;
import dk.gormkrings.event.Type;
import dk.gormkrings.event.date.MonthEvent;
import dk.gormkrings.taxes.TaxRule;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Getter
@Setter
public class DepositPhase extends SimulationPhase {
    private Deposit deposit;
    private boolean firstTime = true;

    public DepositPhase(Phase precedingPhase, long durationInMonths, Deposit deposit, TaxRule taxRule) {
        System.out.println("Initializing Deposit Phase");
        setName("Deposit");

        LocalDate startDate = precedingPhase.getStartDate().plusDays(precedingPhase.getDuration());
        LocalDate endDate = startDate.plusMonths(durationInMonths);
        int days = (int) startDate.until(endDate, ChronoUnit.DAYS);

        this.setStartDate(startDate);
        this.deposit = deposit;
        this.setDuration(days);
        this.setLiveData(precedingPhase.getLiveData());
        this.setTaxRule(taxRule);
    }

    public DepositPhase(LocalDate startDate, long durationInMonths, Deposit deposit, LiveData liveData, TaxRule taxRule) {
        System.out.println("Initializing Deposit Phase");
        setName("Deposit");

        LocalDate endDate = startDate.plusMonths(durationInMonths);
        int days = (int) startDate.until(endDate, ChronoUnit.DAYS);

        this.setStartDate(startDate);
        this.deposit = deposit;
        this.setDuration(days);
        this.setLiveData(liveData);
        this.setTaxRule(taxRule);
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        MonthEvent monthEvent = (MonthEvent) event;
        if (monthEvent.getType() != Type.END) return;

        LiveData data = getLiveData();
        depositMoney(data);
        printPretty(data);
    }

    public void depositMoney(LiveData data) {
        if (firstTime) {
            data.addToDeposit(deposit.getInitial());
            data.addToCapital(deposit.getInitial());
            firstTime = false;
        }
        data.addToDeposit(deposit.getMonthly());
        data.addToCapital(deposit.getMonthly());
        deposit.increaseMonthly(deposit.getMonthlyIncrease());
    }

    public void printPretty(LiveData data) {
        System.out.println(getPrettyCurrentDate() +
                " - Monthly " + formatNumber(deposit.getMonthly()) +
                " - Deposited " + formatNumber(data.getDeposit()) +
                " - Capital " + formatNumber(data.getCapital()));
    }

    @Override
    public Phase copy() {
        return new DepositPhase(
                this.getStartDate(),
                getDuration(),
                this.deposit.copy(),
                getLiveData().copy(),
                getTaxRule().copy());
    }
}
