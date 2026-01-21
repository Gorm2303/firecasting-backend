package dk.gormkrings.randomNumberGenerator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultRandomNumberGeneratorSeedTest {

    @Test
    void sameNonNegativeSeedProducesSameSequence() {
        long seed = 42L;

        DefaultRandomNumberGenerator a = new DefaultRandomNumberGenerator(seed);
        DefaultRandomNumberGenerator b = new DefaultRandomNumberGenerator(seed);

        for (int i = 0; i < 10; i++) {
            assertEquals(a.nextDouble(), b.nextDouble(), "Mismatch at index " + i);
        }
    }

    @Test
    void negativeSeedIsStochasticEvenWhenRepeated() {
        DefaultRandomNumberGenerator a = new DefaultRandomNumberGenerator(-1L);
        DefaultRandomNumberGenerator b = new DefaultRandomNumberGenerator(-1L);

        double[] sa = new double[]{a.nextDouble(), a.nextDouble(), a.nextDouble()};
        double[] sb = new double[]{b.nextDouble(), b.nextDouble(), b.nextDouble()};

        boolean anyDiff = false;
        for (int i = 0; i < sa.length; i++) {
            if (Double.compare(sa[i], sb[i]) != 0) {
                anyDiff = true;
                break;
            }
        }

        assertTrue(anyDiff, "Expected at least one sampled value to differ for repeated negative seed");
    }

    @Test
    void differentNegativeSeedsAreStillStochastic() {
        DefaultRandomNumberGenerator a = new DefaultRandomNumberGenerator(-123L);
        DefaultRandomNumberGenerator b = new DefaultRandomNumberGenerator(-999L);

        // It's astronomically unlikely that both sequences match exactly.
        assertNotEquals(a.nextDouble(), b.nextDouble());
    }
}
