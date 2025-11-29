package dk.gormkrings.distribution.factory;

import dk.gormkrings.math.distribution.IDistribution;
import dk.gormkrings.math.distribution.IDistributionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DistributionManager implements IDistributionFactory {

    private final Map<String, IDistribution> distributionMap;

    // Spring will auto-wire the Map with all IDistribution beans available in the context.
    public DistributionManager(Map<String, IDistribution> distributionMap) {
        this.distributionMap = distributionMap;
    }

    @Override
    public IDistribution createDistribution(String type) {
        IDistribution distribution = distributionMap.get(type);
        if (distribution == null) {
            throw new IllegalArgumentException("Distribution bean not found: " + type);
        }
        return distribution;
    }
}
