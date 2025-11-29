package dk.gormkrings.distribution.factory;

import dk.gormkrings.distribution.BrownianMotionDistribution;
import dk.gormkrings.distribution.NormalDistribution;
import dk.gormkrings.distribution.RegimeBasedDistribution;
import dk.gormkrings.distribution.TDistributionImpl;
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

    public DistributionFactory(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public IDistribution createDistribution(String type) {
        IDistribution dClass = switch (type) {
            case "brownianMotion" -> context.getBean(BrownianMotionDistribution.class);
            case "tDistribution" -> context.getBean(TDistributionImpl.class);
            case "regimeBased" -> context.getBean(RegimeBasedDistribution.class);
            case "normal" -> context.getBean(NormalDistribution.class);
            default -> throw new IllegalArgumentException("Unknown distribution type: " + type);
        };

        log.info("Creating new {} distribution", type.toUpperCase());
        return dClass;
    }


}
