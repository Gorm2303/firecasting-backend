package dk.gormkrings.taxes;

import dk.gormkrings.event.action.WithdrawEvent;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;

@Setter
@Getter
public class CapitalGainsTax implements TaxRule, SmartApplicationListener {
    private float taxRate;
    private StockExemptionTax stockExemptionTax;
    private TaxExemptionCard taxExemptionCard;

    @Override
    public double calculateTax() {
        return 0;
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
