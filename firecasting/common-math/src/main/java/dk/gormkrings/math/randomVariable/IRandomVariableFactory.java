package dk.gormkrings.math.randomVariable;

import dk.gormkrings.math.distribution.IDistribution;
import dk.gormkrings.math.randomNumberGenerator.IRandomNumberGenerator;

public interface IRandomVariableFactory {
    IRandomVariable createRandomVariable(IRandomNumberGenerator generator, IDistribution distribution);
}
