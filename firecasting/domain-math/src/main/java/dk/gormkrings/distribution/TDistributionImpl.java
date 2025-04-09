package dk.gormkrings.distribution;

import dk.gormkrings.math.distribution.IDistribution;
import dk.gormkrings.math.randomNumberGenerator.IRandomNumberGenerator;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.math3.distribution.TDistribution;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "distribution.t")
@Setter
@Getter
public class TDistributionImpl implements IDistribution {
    private TDistribution tDistribution;
    private double mu;
    private double sigma;
    private double nu;
    private double dt;

    @PostConstruct
    public void init() {
        this.tDistribution = new TDistribution(nu);
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
        TDistributionImpl copy = new TDistributionImpl();
        // Ensure properties are copied, then reinitialize the distribution on the copy
        copy.mu = this.mu;
        copy.sigma = this.sigma;
        copy.nu = this.nu;
        copy.dt = this.dt;
        copy.tDistribution = new TDistribution(copy.nu);
        return copy;
    }

    @Override
    public String toString() {
        return "TDistributionImpl{" +
                ", mu=" + mu +
                ", sigma=" + sigma +
                ", nu=" + nu +
                ", dt=" + dt +
                '}';
    }
}
