package dk.gormkrings.randomNumberGenerator;

import dk.gormkrings.math.randomNumberGenerator.IRandomNumberGenerator;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
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
