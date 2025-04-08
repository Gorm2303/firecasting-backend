package dk.gormkrings.distribution;

import dk.gormkrings.math.distribution.IDistribution;
import dk.gormkrings.math.randomNumberGenerator.IRandomNumberGenerator;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "distribution.brownian")
@Setter
@Getter
public class BrownianMotionDistribution implements IDistribution {
    /**
     * drift      the drift (expected return) for the stock
     * volatility the volatility (standard deviation) for the stock
     * dt         the time step to simulate (for daily simulation, use 1/252)
     */
    private double drift;
    private double volatility;
    private double dt; // time step

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
        return new BrownianMotionDistribution();
    }
}
