package dk.gormkrings.math.randomNumberGenerator;

public interface IRandomNumberGeneratorFactory {
    IRandomNumberGenerator createRandomNumberGenerator(long seed);
}
