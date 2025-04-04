package dk.gormkrings.distribution;

import dk.gormkrings.math.distribution.IDistribution;
import dk.gormkrings.math.distribution.IDistributionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component("distributionFactory")
public class DistributionFactory implements IDistributionFactory {

    private final ApplicationContext context;

    @Value("${domain.distributionReturn.distribution:brownianMotionDistribution}")
    private String distribution;

    public DistributionFactory(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public IDistribution createDistribution() {
        if ("normalDistribution".equalsIgnoreCase(distribution)) {
            log.info("Creating new Normal Distribution");
            return context.getBean(NormalDistribution.class);
        } else if ("tDistribution".equalsIgnoreCase(distribution)) {
            log.info("Creating new T Distribution");
            return context.getBean(TDistributionImpl.class);
        } else {
            log.info("Creating new Brownian Motion Distribution");
            return context.getBean(BrownianMotionDistribution.class);
        }
    }
}
