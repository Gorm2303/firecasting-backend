package dk.gormkrings.simulation.phases;

import dk.gormkrings.action.Deposit;
import dk.gormkrings.data.LiveData;
import dk.gormkrings.event.Type;
import dk.gormkrings.event.date.MonthEvent;
import dk.gormkrings.investment.Return;
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
        super(previousPhase.getLiveData(), startDate,duration,taxRule, previousPhase.getReturner(), "Deposit");
        System.out.println("Initializing Additional Deposit Phase");
        this.deposit = deposit;
    }

    public DepositPhase(LocalDate startDate, long duration, Deposit deposit, LiveData liveData, Return returner, TaxRule taxRule) {
        super(liveData, startDate, duration, taxRule, returner, "Deposit");
        System.out.println("Initializing Deposit Phase");
        this.deposit = deposit;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        MonthEvent monthEvent = (MonthEvent) event;
        if (monthEvent.getType() != Type.END) return;

        LiveData data = monthEvent.getData();
        addReturn(data);
        depositMoney(data);
        System.out.println(prettyString(data));
    }

    public void depositMoney(LiveData data) {
        if (firstTime) {
            data.addToDeposited(deposit.getInitial());
            data.addToCapital(deposit.getInitial());
            firstTime = false;
        }
        double depositAmount = deposit.getMonthly();
        data.setDeposit(depositAmount);
        data.addToDeposited(depositAmount);
        data.addToCapital(depositAmount);
        deposit.increaseMonthly(deposit.getMonthlyIncrease());
    }

    @Override
    public String prettyString(LiveData data) {
        return super.prettyString(data) +
                " - Deposit " + formatNumber(data.getDeposit()
        );
    }

    @Override
    public Phase copy(Phase previousPhase) {
        if (previousPhase == null) {
            return new DepositPhase(
                    this.getStartDate(),
                    getDuration(),
                    this.deposit.copy(),
                    getLiveData().copy(),
                    getReturner().copy(),
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
