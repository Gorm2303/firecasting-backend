package dk.gormkrings.distribution;

import dk.gormkrings.math.distribution.IDistribution;
import dk.gormkrings.math.distribution.IDistributionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component("distributionFactory")
public class DistributionFactory implements IDistributionFactory {
    @Value("${domain.distributionReturn.distribution:brownianMotionDistribution}")
    private String distribution;

    public IDistribution createDistribution() {
        if ("normalDistribution".equalsIgnoreCase(distribution)) {
            log.info("Creating new Normal Distribution");
            return new NormalDistribution();
        } else {
            log.info("Creating new Brownian Motion Distribution");
            return new BrownianMotionDistribution();
        }
    }
}
