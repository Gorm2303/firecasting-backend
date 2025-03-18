package dk.gormkrings.simulation.randomness;

public class DefaultRandomVariable implements RandomVariable {
    private final Distribution distribution;
    private final RandomNumberGenerator rng;

    public DefaultRandomVariable() {
        this.distribution = new BrownianMotionDistribution(0, 0.2);
        this.rng = new DefaultRandomNumberGenerator();
    }

    public DefaultRandomVariable(Distribution distribution, RandomNumberGenerator rng) {
        this.distribution = distribution;
        this.rng = rng;
    }

    @Override
    public double sample() {
        return distribution.sample(rng);
    }
}

