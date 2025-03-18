package dk.gormkrings;

import dk.gormkrings.event.date.SimulationMonthEvent;
import dk.gormkrings.event.date.Type;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Getter
@Component
public class Deposit {
    private double initial;
    @Setter
    private double monthly;
    private double total;

    public Deposit() {
        this.initial = 0;
        this.monthly = 5000;
        this.total = initial;
    }

    @EventListener
    public void onSimulationUpdate(SimulationMonthEvent event) {
        if (event.getType() != Type.END) return;
        int currentDay = event.getData().getCurrentTimeSpan();
        System.out.println("Day " + currentDay + ": Depositing money.");

        monthlyIncrement();
    }

    public void monthlyIncrement() {
        total += monthly;
    }

    public void setInitial(double initial) {
        this.initial = initial;
        total = initial;
    }
}
