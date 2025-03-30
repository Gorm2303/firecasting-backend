package dk.gormkrings.inflation;

import org.springframework.stereotype.Component;

@Component
public class DefaultInflationFactory implements IInflationFactory {
    @Override
    public IInflation createInflation() {
        return new DataAverageInflation();
    }

    @Override
    public IInflation createInflation(float inflationPercentage) {
        return new DataAverageInflation();
    }
}
