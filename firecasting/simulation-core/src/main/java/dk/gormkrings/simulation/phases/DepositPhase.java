package dk.gormkrings.simulation.phases;

import dk.gormkrings.Deposit;
import dk.gormkrings.event.date.SimulationMonthEvent;
import dk.gormkrings.event.date.Type;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class DepositPhase extends SimulationPhase {
    private Deposit deposit;

    public DepositPhase() {
        setName("Deposit Phase");
    }

    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        if (!(event instanceof SimulationMonthEvent monthEvent)) return;

        if (monthEvent.getType() != Type.END) return;

        int currentDay = monthEvent.getData().getCurrentTimeSpan();
        System.out.println("Day " + currentDay + ": Depositing money.");
        deposit.addMonthly();
    }

}
