package dk.gormkrings.taxes;

import dk.gormkrings.event.date.SimulationYearEvent;
import dk.gormkrings.event.date.Type;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class NotionalGainsTax implements TaxRule {
    @Override
    public double calculateTax() {
        return 0;
    }

    @EventListener
    public void onSimulationUpdate(SimulationYearEvent event) {
        if (event.getType() != Type.END) return;
        int day = event.getData().getCurrentTimeSpan();
        System.out.println("Year " + (day / 365) + ": Executing tax calculations.");
        // Implement your tax calculation logic here
    }
}
