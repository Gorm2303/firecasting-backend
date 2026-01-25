package dk.gormkrings.randomNumberGenerator;

import dk.gormkrings.math.randomNumberGenerator.IRandomNumberGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;

@Component
@Scope("prototype")
public class DefaultRandomNumberGenerator implements IRandomNumberGenerator {
    private final SplittableRandom random;
    private long seed = 1; // Negative values makes it stochastic

    public DefaultRandomNumberGenerator() {
        this.seed = -1;
        this.random = new SplittableRandom();
    }

    private DefaultRandomNumberGenerator(SplittableRandom random) {
        this.seed = -1;
        this.random = random.split();
    }

    public DefaultRandomNumberGenerator(long seed) {
        if (seed < 0) {
            // Negative seed means: stochastic/unseeded (even if repeated).
            this.seed = -1;
            this.random = new SplittableRandom(ThreadLocalRandom.current().nextLong());
        } else {
            this.seed = seed;
            this.random = new SplittableRandom(seed);
        }
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
        // Deterministically split a new independent stream.
        // This avoids seed++ coupling that makes results depend on copy order.
        return new DefaultRandomNumberGenerator(random);
    }
}
