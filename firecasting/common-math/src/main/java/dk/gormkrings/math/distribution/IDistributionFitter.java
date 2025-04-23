package dk.gormkrings.math.distribution;

import java.util.List;

public interface IDistributionFitter {
    IDistribution fitNormalDistribution(List<Double> returns, double dt);
    IDistribution fitBrownianMotionDistribution(List<Double> returns, double dt);
    IDistribution fitTDistribution(List<Double> returns, double dt);
}
