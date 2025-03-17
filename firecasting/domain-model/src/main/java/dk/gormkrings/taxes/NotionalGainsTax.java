package dk.gormkrings.taxes;

import dk.gormkrings.simulation.SimulationUpdateEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class NotionalGainsTax implements TaxRule {
    @Override
    public double calculateTax() {
        return 0;
    }

    @EventListener
    public void onSimulationUpdate(SimulationUpdateEvent event) {
        int day = event.getDay();
        if (day % 365 == 0 && day != 0) {
            System.out.println("Day " + day + ": Executing tax calculations.");
            // Implement your tax calculation logic here
        }
    }
}
