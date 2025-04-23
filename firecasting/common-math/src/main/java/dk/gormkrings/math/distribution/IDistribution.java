package dk.gormkrings.math.distribution;

import dk.gormkrings.math.randomNumberGenerator.IRandomNumberGenerator;

public interface IDistribution {
    double sample(IRandomNumberGenerator rng);
    IDistribution copy();
    String toString();
}
