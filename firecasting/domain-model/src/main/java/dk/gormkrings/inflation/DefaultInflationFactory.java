package dk.gormkrings.inflation;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class DefaultInflationFactory implements IInflationFactory {

    private final ApplicationContext context;

    public DefaultInflationFactory(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public IInflation createInflation() {
        return context.getBean(DataAverageInflation.class);
    }

    @Override
    public IInflation createInflation(double inflationPercentage) {
        return new DataAverageInflation(inflationPercentage);
    }
}
