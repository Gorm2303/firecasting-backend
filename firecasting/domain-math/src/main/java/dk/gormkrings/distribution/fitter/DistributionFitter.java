package dk.gormkrings.distribution.fitter;

import dk.gormkrings.distribution.BrownianMotionDistribution;
import dk.gormkrings.distribution.factory.DistributionFactory;
import dk.gormkrings.distribution.NormalDistribution;
import dk.gormkrings.distribution.TDistributionImpl;
import dk.gormkrings.math.distribution.IDistributionFitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DistributionFitter implements IDistributionFitter {

    private final DistributionFactory factory;

    // Inject the number of trading days per year from your properties (e.g., trading.days.per.year=252)
    @Value("${trading.days.per.year}")
    private int tradingDaysPerYear;

    public DistributionFitter(DistributionFactory distributionFactory) {
        this.factory = distributionFactory;
    }

    @Override
    public NormalDistribution fitNormalDistribution(List<Double> returns, double dt) {
        double dailyMean = computeMean(returns);
        double dailyVariance = computeVariance(returns, dailyMean);
        double dailyStdDev = Math.sqrt(dailyVariance);

        // Annualize the daily parameters
        double annualMean = dailyMean * tradingDaysPerYear;
        double annualStdDev = dailyStdDev * Math.sqrt(tradingDaysPerYear);

        NormalDistribution nd = (NormalDistribution) factory.createDistribution("normal");
        nd.setMean(annualMean);
        nd.setStandardDeviation(annualStdDev);
        nd.setDt(dt);  // dt is passed in from another configuration
        return nd;
    }

    @Override
    public BrownianMotionDistribution fitBrownianMotionDistribution(List<Double> returns, double dt) {
        double dailyDrift = computeMean(returns);
        double dailyVariance = computeVariance(returns, dailyDrift);
        double dailyVolatility = Math.sqrt(dailyVariance);

        // Annualize the drift and volatility
        double annualDrift = dailyDrift * tradingDaysPerYear;
        double annualVolatility = dailyVolatility * Math.sqrt(tradingDaysPerYear);

        BrownianMotionDistribution brownian = (BrownianMotionDistribution) factory.createDistribution("brownianMotion");
        brownian.setDrift(annualDrift);
        brownian.setVolatility(annualVolatility);
        brownian.setDt(dt);  // dt set externally
        return brownian;
    }

    @Override
    public TDistributionImpl fitTDistribution(List<Double> returns, double dt) {
        double dailyMean = computeMean(returns);
        double dailyVariance = computeVariance(returns, dailyMean);
        double dailySigma = Math.sqrt(dailyVariance);

        // Annualize the mean and sigma
        double annualMean = dailyMean * tradingDaysPerYear;
        double annualSigma = dailySigma * Math.sqrt(tradingDaysPerYear);

        TDistributionImpl tDistribution = (TDistributionImpl) factory.createDistribution("tDistribution");
        tDistribution.setMu(annualMean);
        tDistribution.setSigma(annualSigma);
        tDistribution.setDt(dt);  // dt is provided externally
        return tDistribution;
    }

    private double computeMean(List<Double> returns) {
        return returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private double computeVariance(List<Double> returns, double mean) {
        return returns.stream().mapToDouble(r -> Math.pow(r - mean, 2)).sum() / (returns.size() - 1);
    }
}
