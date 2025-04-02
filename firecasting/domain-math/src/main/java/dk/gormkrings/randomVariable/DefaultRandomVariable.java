package dk.gormkrings.randomVariable;

import dk.gormkrings.math.distribution.IDistribution;
import dk.gormkrings.math.distribution.IDistributionFactory;
import dk.gormkrings.math.randomNumberGenerator.IRandomNumberGenerator;
import dk.gormkrings.math.randomVariable.IRandomVariable;
import dk.gormkrings.randomNumberGenerator.DefaultRandomNumberGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component("defaultRandomVariable")
public class DefaultRandomVariable implements IRandomVariable {
    private final IDistribution distribution;
    private final IRandomNumberGenerator rng;

    @Autowired
    public DefaultRandomVariable(IDistributionFactory distributionFactory) {
        this.rng = new DefaultRandomNumberGenerator();
        distribution = distributionFactory.createDistribution();
    }

    public DefaultRandomVariable(IDistribution distribution, IRandomNumberGenerator rng) {
        this.distribution = distribution;
        this.rng = rng;
    }

    @Override
    public double sample() {
        return distribution.sample(rng);
    }

    @Override
    public IRandomVariable copy() {
        return new DefaultRandomVariable(
                distribution.copy(),
                rng.copy()
        );
    }
}

