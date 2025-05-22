package dk.gormkrings.randomVariable;

import dk.gormkrings.math.distribution.IDistribution;
import dk.gormkrings.math.distribution.IDistributionFactory;
import dk.gormkrings.math.randomNumberGenerator.IRandomNumberGenerator;
import dk.gormkrings.math.randomVariable.IRandomVariable;
import dk.gormkrings.randomNumberGenerator.DefaultRandomNumberGenerator;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Slf4j
@Component("defaultRandomVariable")
@Scope("prototype")
@Getter
@Setter
public class DefaultRandomVariable implements IRandomVariable {
    private IDistribution distribution;
    private IRandomNumberGenerator randomNumberGenerator;

    public DefaultRandomVariable() {}

    @Autowired
    public DefaultRandomVariable(IDistributionFactory distributionFactory) {
        this.randomNumberGenerator = new DefaultRandomNumberGenerator();
        distribution = distributionFactory.createDistribution();
    }

    public DefaultRandomVariable(IDistribution distribution, IRandomNumberGenerator rng) {
        this.distribution = distribution;
        this.randomNumberGenerator = rng;
    }

    @Override
    public double sample() {
        return distribution.sample(randomNumberGenerator);
    }

    @Override
    public IRandomVariable copy() {
        return new DefaultRandomVariable(
                distribution.copy(),
                randomNumberGenerator.copy()
        );
    }
}

