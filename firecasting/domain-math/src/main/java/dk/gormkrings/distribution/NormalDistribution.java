package dk.gormkrings.distribution;

import dk.gormkrings.math.distribution.IDistribution;
import dk.gormkrings.math.randomNumberGenerator.IRandomNumberGenerator;

public class NormalDistribution implements IDistribution {
    private final double mean;
    private final double standardDeviation;
    private final double dt; // time step

    public NormalDistribution() {
        this(0.07, 0.20, 1.0 / 252);
    }

    /**
     * Constructs a NormalDistribution with the specified mean, standard deviation, and time step.
     *
     * @param mean              the mean (annualized if dt is a fraction of a year)
     * @param standardDeviation the standard deviation (annualized if dt is a fraction of a year)
     * @param dt                the time step (for daily simulation, use 1/252)
     */
    public NormalDistribution(double mean, double standardDeviation, double dt) {
        this.mean = mean;
        this.standardDeviation = standardDeviation;
        this.dt = dt;
    }

    @Override
    public double sample(IRandomNumberGenerator rng) {
        double u1 = rng.nextDouble();
        double u2 = rng.nextDouble();
        // Box-Muller transform to generate a standard normal variable Z
        double standardNormal = Math.sqrt(-2 * Math.log(u1)) * Math.cos(2 * Math.PI * u2);
        // Scale by dt: mean * dt + standardDeviation * sqrt(dt) * Z
        return mean * dt + standardDeviation * Math.sqrt(dt) * standardNormal;
    }

    @Override
    public IDistribution copy() {
        return new NormalDistribution(mean, standardDeviation, dt);
    }
}
