package dk.gormkrings.inflation;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

public interface Inflation extends ApplicationListener<ApplicationEvent> {
    double calculateInflation(double amount);
    Inflation copy();
}
