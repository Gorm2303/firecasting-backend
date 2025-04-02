package dk.gormkrings.math.randomNumberGenerator;

public interface IRandomNumberGenerator {
    double nextDouble();
    IRandomNumberGenerator copy();
}
