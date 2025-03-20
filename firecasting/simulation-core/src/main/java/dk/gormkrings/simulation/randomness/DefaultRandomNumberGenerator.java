package dk.gormkrings.simulation.randomness;

import java.util.Random;

public class DefaultRandomNumberGenerator implements RandomNumberGenerator {
    private final Random random;

    public DefaultRandomNumberGenerator() {
        this.random = new Random();
    }

    public DefaultRandomNumberGenerator(long seed) {
        this.random = new Random(seed);
    }

    @Override
    public double nextDouble() {
        return random.nextDouble();
    }
}
