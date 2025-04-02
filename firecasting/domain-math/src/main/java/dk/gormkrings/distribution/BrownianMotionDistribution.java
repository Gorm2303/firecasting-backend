package dk.gormkrings.distribution;

import dk.gormkrings.math.distribution.IDistribution;
import dk.gormkrings.math.randomNumberGenerator.IRandomNumberGenerator;

public class BrownianMotionDistribution implements IDistribution {
    private final double drift;
    private final double volatility;
    private final double dt; // time step

    public BrownianMotionDistribution() {
        this(0.07, 0.20, 1.0/252);
    }

    /**
     * Constructs a BrownianMotionDistribution with the given drift, volatility, and time step.
     *
     * @param drift      the drift (expected return) for the stock
     * @param volatility the volatility (standard deviation) for the stock
     * @param dt         the time step to simulate (for daily simulation, use 1/252)
     */
    public BrownianMotionDistribution(double drift, double volatility, double dt) {
        this.drift = drift;
        this.volatility = volatility;
        this.dt = dt;
    }

    @Override
    public double sample(IRandomNumberGenerator rng) {
        double u1 = rng.nextDouble();
        double u2 = rng.nextDouble();
        // Box-Muller transform to generate a standard normal variable Z
        double standardNormal = Math.sqrt(-2 * Math.log(u1)) * Math.cos(2 * Math.PI * u2);
        return (drift - 0.5 * volatility * volatility) * dt + volatility * Math.sqrt(dt) * standardNormal;
    }

    @Override
    public IDistribution copy() {
        return new BrownianMotionDistribution(drift, volatility, dt);
    }
}
