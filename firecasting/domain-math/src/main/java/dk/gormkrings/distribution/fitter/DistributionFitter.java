package dk.gormkrings.distribution.fitter;

import dk.gormkrings.distribution.BrownianMotionDistribution;
import dk.gormkrings.distribution.factory.DistributionFactory;
import dk.gormkrings.distribution.NormalDistribution;
import dk.gormkrings.distribution.TDistributionImpl;
import dk.gormkrings.math.distribution.IDistributionFitter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DistributionFitter implements IDistributionFitter {

    private final DistributionFactory factory;

    public DistributionFitter(DistributionFactory distributionFactory) {
        this.factory = distributionFactory;
    }

    @Override
    public NormalDistribution fitNormalDistribution(List<Double> returns, double dt) {
        double mean = computeMean(returns);
        double stdDev = Math.sqrt(computeVariance(returns, mean));

        NormalDistribution nd = (NormalDistribution) factory.createDistribution("normal");
        nd.setMean(mean);
        nd.setStandardDeviation(stdDev);
        nd.setDt(dt);
        return nd;
    }

    @Override
    public BrownianMotionDistribution fitBrownianMotionDistribution(List<Double> returns, double dt) {
        double drift = computeMean(returns);
        double volatility = Math.sqrt(computeVariance(returns, drift));

        BrownianMotionDistribution brownian = (BrownianMotionDistribution) factory.createDistribution("brownianMotion");
        brownian.setDrift(drift);
        brownian.setVolatility(volatility);
        brownian.setDt(dt);
        return brownian;
    }

    @Override
    public TDistributionImpl fitTDistribution(List<Double> returns, double dt) {
        double mean = computeMean(returns);
        double sigma = Math.sqrt(computeVariance(returns, mean));

        TDistributionImpl tDistribution = (TDistributionImpl) factory.createDistribution("tDistribution");
        tDistribution.setMu(mean);
        tDistribution.setSigma(sigma);
        tDistribution.setDt(dt);
        return tDistribution;
    }

    private double computeMean(List<Double> returns) {
        return returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private double computeVariance(List<Double> returns, double mean) {
        return returns.stream().mapToDouble(r -> Math.pow(r - mean, 2)).sum() / (returns.size() - 1);
    }
}
