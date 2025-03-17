package dk.gormkrings.randomness;

public class BrownianMotionDistribution implements Distribution {
    private final double drift;
    private final double volatility;

    /**
     * Constructs a BrownianMotionDistribution with the given drift and volatility.
     *
     * @param drift      the drift (mean shift) for the distribution
     * @param volatility the volatility (standard deviation) for the distribution
     */
    public BrownianMotionDistribution(double drift, double volatility) {
        this.drift = drift;
        this.volatility = volatility;
    }

    @Override
    public double sample(RandomNumberGenerator rng) {
        double u1 = rng.nextDouble();
        double u2 = rng.nextDouble();
        // Box-Muller transform to generate a standard normal variable
        double standardNormal = Math.sqrt(-2 * Math.log(u1)) * Math.cos(2 * Math.PI * u2);
        return drift + volatility * standardNormal;
    }
}
