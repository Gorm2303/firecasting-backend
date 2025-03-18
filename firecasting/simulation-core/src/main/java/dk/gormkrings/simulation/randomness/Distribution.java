package dk.gormkrings.simulation.randomness;

public interface Distribution {
    double sample(RandomNumberGenerator rng);
}
