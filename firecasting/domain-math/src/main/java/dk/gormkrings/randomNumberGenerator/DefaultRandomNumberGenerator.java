package dk.gormkrings.randomNumberGenerator;

import dk.gormkrings.math.randomNumberGenerator.IRandomNumberGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.SplittableRandom;

@Component
@Scope("prototype")
public class DefaultRandomNumberGenerator implements IRandomNumberGenerator {
    private final SplittableRandom random;
    private long seed = 1; // Negative values makes it stochastic

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
        if (seed < 0) {
            return new DefaultRandomNumberGenerator(random);
        }
        seed++;
        return new DefaultRandomNumberGenerator(seed);
    }
}
