package dk.gormkrings.inflation;

import org.springframework.stereotype.Component;

@Component
public class DefaultInflationFactory implements IInflationFactory {
    @Override
    public IInflation createInflation() {
        return new DataAverageInflation();
    }

    @Override
    public IInflation createInflation(double inflationPercentage) {
        return new DataAverageInflation(inflationPercentage);
    }
}
