package dk.gormkrings.taxes;

import dk.gormkrings.event.Type;
import dk.gormkrings.event.date.YearEvent;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

@Slf4j
public class NotionalGainsTax implements TaxRule {
    @Override
    public double calculateTax() {
        return 0;
    }

    @Override
    public TaxRule copy() {
        return new NotionalGainsTax();
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        YearEvent yearEvent = (YearEvent) event;
        if (yearEvent.getType() != Type.END) return;

        log.debug("Year " + (yearEvent.getData().getSessionDuration() / 365) + ": NotionalGainsTax calculating tax.");
        // Implement your tax calculation logic here
    }

    @Override
    public boolean supportsEventType(@NonNull Class<? extends ApplicationEvent> eventType) {
        return YearEvent.class.isAssignableFrom(eventType);
    }

    @Override
    public boolean supportsSourceType(Class<?> sourceType) {
        return true;
    }

}
