package dk.gormkrings.distribution;

import dk.gormkrings.math.distribution.IDistribution;
import dk.gormkrings.math.randomNumberGenerator.IRandomNumberGenerator;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "distribution.normal")
@Setter
@Getter
public class NormalDistribution implements IDistribution {
    /**
     * Constructs a NormalDistribution with the specified mean, standard deviation, and time step.
     * mean              the mean (annualized if dt is a fraction of a year)
     * standardDeviation the standard deviation (annualized if dt is a fraction of a year)
     * dt                the time step (for daily simulation, use 1/252)
     */
    private double mean;
    private double standardDeviation;
    private double dt; // time step

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
        return new NormalDistribution();
    }
}
