package dk.gormkrings.math.distribution;

public interface IDistributionFactory {
    IDistribution createDistribution();
    IDistribution createDistribution(String type);
}
