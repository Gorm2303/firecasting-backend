package dk.gormkrings.randomVariable;

import dk.gormkrings.distribution.BrownianMotionDistribution;
import dk.gormkrings.math.distribution.IDistribution;
import dk.gormkrings.math.randomNumberGenerator.IRandomNumberGenerator;
import dk.gormkrings.math.randomVariable.IRandomVariable;
import dk.gormkrings.randomNumberGenerator.DefaultRandomNumberGenerator;

public class DefaultRandomVariable implements IRandomVariable {
    private final IDistribution distribution;
    private final IRandomNumberGenerator rng;

    public DefaultRandomVariable() {
        this.distribution = new BrownianMotionDistribution(0, 0.2);
        this.rng = new DefaultRandomNumberGenerator();
    }

    public DefaultRandomVariable(IDistribution distribution, IRandomNumberGenerator rng) {
        this.distribution = distribution;
        this.rng = rng;
    }

    @Override
    public double sample() {
        return distribution.sample(rng);
    }
}

