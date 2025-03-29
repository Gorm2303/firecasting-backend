package dk.gormkrings.inflation;

import org.springframework.stereotype.Component;

@Component
public class DefaultInflationFactory implements IInflationFactory {
    @Override
    public Inflation createInflation() {
        return new DataAverageInflation();
    }

    @Override
    public Inflation createInflation(float inflationPercentage) {
        return new DataAverageInflation();
    }
}
