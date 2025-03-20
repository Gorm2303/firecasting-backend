package dk.gormkrings.simulation.phases;

import dk.gormkrings.action.Deposit;
import dk.gormkrings.data.LiveData;
import dk.gormkrings.event.Type;
import dk.gormkrings.event.date.MonthEvent;
import dk.gormkrings.taxes.TaxRule;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

@Getter
@Setter
public class DepositPhase extends SimulationPhase {
    private Deposit deposit;
    private boolean firstTime = true;

    public DepositPhase(Phase previousPhase, LocalDate startDate, long duration, Deposit deposit, TaxRule taxRule) {
        super(previousPhase.getLiveData(), startDate,duration,taxRule,"Deposit");
        //System.out.println("Initializing Additional Deposit Phase");
        this.deposit = deposit;
    }

    public DepositPhase(LocalDate startDate, long duration, Deposit deposit, LiveData liveData, TaxRule taxRule) {
        super(liveData, startDate, duration, taxRule,"Deposit");
        //System.out.println("Initializing Deposit Phase");
        this.deposit = deposit;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        MonthEvent monthEvent = (MonthEvent) event;
        if (monthEvent.getType() != Type.END) return;

        LiveData data = getLiveData();
        depositMoney(data);
        //printPretty(data);
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
    public Phase copy(Phase previousPhase) {
        if (previousPhase == null) {
            return new DepositPhase(
                    this.getStartDate(),
                    getDuration(),
                    this.deposit.copy(),
                    getLiveData().copy(),
                    getTaxRule().copy());
        } else {
            return new DepositPhase(
                    previousPhase,
                    this.getStartDate(),
                    getDuration(),
                    this.deposit.copy(),
                    getTaxRule().copy());
        }
    }
}
