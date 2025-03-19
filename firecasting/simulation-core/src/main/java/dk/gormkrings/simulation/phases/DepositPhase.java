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

import java.text.DecimalFormat;

@Getter
@Setter
public class DepositPhase extends SimulationPhase {
    private Deposit deposit;
    private boolean firstTime = true;
    private TaxRule taxRule;

    public DepositPhase() {
        setName("Deposit");
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
        deposit.increaseMonthly();
        data.addToDeposit(deposit.getMonthly());
        data.addToCapital(deposit.getMonthly());
    }

    public void printPretty(LiveData data) {
        System.out.println(getPrettyCurrentDate() +
                " - Monthly " + formatNumber(deposit.getMonthly()) +
                " - Deposited " + formatNumber(data.getDeposit()) +
                " - Capital " + formatNumber(data.getCapital()));
    }
}
