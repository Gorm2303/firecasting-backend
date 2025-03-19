package dk.gormkrings.simulation.phases;

import dk.gormkrings.Deposit;
import dk.gormkrings.data.LiveData;
import dk.gormkrings.event.Type;
import dk.gormkrings.event.date.MonthEvent;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;
import org.springframework.stereotype.Component;

@Getter
@Setter
public class DepositPhase extends SimulationPhase {
    private Deposit deposit;

    public DepositPhase() {
        setName("Deposit Phase");
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
        deposit.increaseMonthly();
        deposit.addMonthly();
        data.addToDeposit(deposit.getMonthly());
        data.addToCapital(deposit.getMonthly());
    }

    public void printPretty(LiveData data) {
        System.out.println();
        System.out.println(getPrettyCurrentDate());
        System.out.println("Depositing money: " + deposit.getMonthly() +
                ", Total Deposit: " + data.getDeposit() +
                ", Capital: " + data.getCapital());

    }


}
