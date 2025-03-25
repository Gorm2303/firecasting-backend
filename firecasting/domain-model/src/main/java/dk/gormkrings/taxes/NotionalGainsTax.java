package dk.gormkrings.taxes;

import dk.gormkrings.data.LiveData;
import dk.gormkrings.event.Type;
import dk.gormkrings.event.date.YearEvent;
import dk.gormkrings.util.Util;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

@Slf4j
public class NotionalGainsTax implements TaxRule {
    private final double taxRate;
    private double previousReturn = 0;


    public NotionalGainsTax(double taxRate) {
        this.taxRate = taxRate;
    }

    @Override
    public double calculateTax(double amount) {
        return amount * taxRate / 100;
    }

    @Override
    public TaxRule copy() {
        return new NotionalGainsTax(taxRate);
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        YearEvent yearEvent = (YearEvent) event;
        if (yearEvent.getType() != Type.END) return;
        LiveData data = (LiveData) yearEvent.getData();

        // Implement your tax calculation logic here
        double tax = calculateTax(data.getReturned() - previousReturn);
        data.subtractFromReturned(tax);
        data.subtractFromCapital(tax);
        previousReturn = data.getReturned();
        data.setYearlyTax(tax);
        data.addToTax(tax);

        log.debug("Year {}: NotionalGainsTax calculating tax: {}",
                data.getSessionDuration() / 365,
                Util.formatNumber(data.getYearlyTax()));
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
