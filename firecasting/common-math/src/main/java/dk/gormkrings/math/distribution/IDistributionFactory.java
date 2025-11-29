package dk.gormkrings.math.distribution;

public interface IDistributionFactory {
    IDistribution createDistribution(String type);
}
