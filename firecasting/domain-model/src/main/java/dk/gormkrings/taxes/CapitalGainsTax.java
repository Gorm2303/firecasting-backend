package dk.gormkrings.taxes;

import dk.gormkrings.event.action.WithdrawEvent;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Setter
@Getter
public class CapitalGainsTax implements TaxRule {
    private float taxRate;
    private StockExemptionTax stockExemptionTax;
    private TaxExemptionCard taxExemptionCard;

    @Override
    public double calculateTax(double amount) {
        return 0;
    }

    @Override
    public TaxRule copy() {
        return new CapitalGainsTax();
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        System.out.println("CapitalGainsTax calculating tax " + event);
    }

    @Override
    public boolean supportsEventType(@NonNull Class<? extends ApplicationEvent> eventType) {
        return WithdrawEvent.class.isAssignableFrom(eventType);
    }

    @Override
    public boolean supportsSourceType(Class<?> sourceType) {
        return true;
    }
}
