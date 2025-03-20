package dk.gormkrings.taxes;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

public interface TaxRule extends ApplicationListener<ApplicationEvent> {
    double calculateTax();
    TaxRule copy();
}
