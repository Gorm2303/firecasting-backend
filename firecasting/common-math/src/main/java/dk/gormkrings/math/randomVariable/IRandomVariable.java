package dk.gormkrings.math.randomVariable;

import dk.gormkrings.math.distribution.IDistribution;
import dk.gormkrings.math.randomNumberGenerator.IRandomNumberGenerator;

public interface IRandomVariable {
    double sample();
    IRandomVariable copy();
    IDistribution getDistribution();
    void setDistribution(IDistribution distribution);
    void setRandomNumberGenerator(IRandomNumberGenerator rng);
}
