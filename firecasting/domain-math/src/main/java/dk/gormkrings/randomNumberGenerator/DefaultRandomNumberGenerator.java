package dk.gormkrings.randomNumberGenerator;

import dk.gormkrings.math.randomNumberGenerator.IRandomNumberGenerator;

import java.util.SplittableRandom;

public class DefaultRandomNumberGenerator implements IRandomNumberGenerator {
    private final SplittableRandom random;

    public DefaultRandomNumberGenerator() {
        this.random = new SplittableRandom();
    }

    private DefaultRandomNumberGenerator(SplittableRandom random) {
        this.random = random.split();
    }

    public DefaultRandomNumberGenerator(long seed) {
        this.random = new SplittableRandom(seed);
    }

    @Override
    public double nextDouble() {
        return random.nextDouble();
    }

    @Override
    public IRandomNumberGenerator copy() {
        return new DefaultRandomNumberGenerator(this.random);
    }
}
