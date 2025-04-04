package dk.gormkrings.distribution;

import dk.gormkrings.math.distribution.IDistribution;
import dk.gormkrings.math.randomNumberGenerator.IRandomNumberGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.distribution.TDistribution;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TDistributionImpl implements IDistribution {
    private final TDistribution tDistribution;
    private final double mu;
    private final double sigma;
    private final double nu;
    private final double dt;

    public TDistributionImpl(
            @Value("${distribution.t.mu:0.042}") double mu,
            @Value("${distribution.t.sigma:0.609}") double sigma,
            @Value("${distribution.t.nu:3.60}") double nu,
            @Value("${distribution.t.dt:0.003968254}") double dt) {
        this.mu = mu;
        this.sigma = sigma;
        this.nu = nu;
        this.dt = dt;
        this.tDistribution = new TDistribution(nu);
        log.debug("Initializing TDistributionImpl with mu={}, sigma={}, nu={}, dt={}", mu, sigma, nu, dt);
    }

    @Override
    public double sample(IRandomNumberGenerator rng) {
        double u = rng.nextDouble();
        double tValue = tDistribution.inverseCumulativeProbability(u);
        // Scale the sample to a daily return:
        return mu * dt + sigma * Math.sqrt(dt) * tValue;
    }

    @Override
    public IDistribution copy() {
        return new TDistributionImpl(mu, sigma, nu, dt);
    }
}
