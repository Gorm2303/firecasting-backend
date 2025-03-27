package dk.gormkrings.randomNumberGenerator;

import dk.gormkrings.math.randomNumberGenerator.IRandomNumberGenerator;

import java.util.Random;

public class DefaultRandomNumberGenerator implements IRandomNumberGenerator {
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
