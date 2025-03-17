package dk.gormkrings.randomness;

public interface Distribution {
    double sample(RandomNumberGenerator rng);
}
