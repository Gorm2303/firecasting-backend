package dk.gormkrings.taxes;

import dk.gormkrings.event.Type;
import dk.gormkrings.event.date.YearEvent;
import lombok.NonNull;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;

public class NotionalGainsTax implements TaxRule, SmartApplicationListener {
    @Override
    public double calculateTax() {
        return 0;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        YearEvent yearEvent = (YearEvent) event;
        if (yearEvent.getType() != Type.END) return;

        long day = yearEvent.getData().getSessionDuration();
        System.out.println("Year " + (day / 365) + ": NotionalGainsTax calculating tax.");
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
