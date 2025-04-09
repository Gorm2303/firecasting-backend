package dk.gormkrings.distribution;

import dk.gormkrings.math.randomNumberGenerator.IRandomNumberGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class BrownianMotionDistributionTest {
    @Test
    public void testSampleCorrectComputation() {
        double u1 = 0.5;
        double u2 = 0.25;
        double drift = 1.0;
        double volatility = 2.0;

        BrownianMotionDistribution distribution = new BrownianMotionDistribution();
        distribution.setDrift(drift);
        distribution.setVolatility(volatility);
        distribution.setDt(1);

        IRandomNumberGenerator rng = mock(IRandomNumberGenerator.class);
        when(rng.nextDouble()).thenReturn(u1, u2);

        double standardNormal = Math.sqrt(-2 * Math.log(u1)) * Math.cos(2 * Math.PI * u2);
        double expected = (drift - 0.5 * volatility * volatility) + volatility * standardNormal;
        double actual = distribution.sample(rng);

        assertEquals(expected, actual, 1e-9, "The sample method should compute the correct value using the Box-Muller transform");
    }

    @Test
    public void testNextDoubleInvocationCount() {
        double drift = 0.0;
        double volatility = 1.0;

        BrownianMotionDistribution distribution = new BrownianMotionDistribution();

        IRandomNumberGenerator rng = mock(IRandomNumberGenerator.class);
        when(rng.nextDouble()).thenReturn(0.5, 0.5);
        distribution.sample(rng);

        verify(rng, times(2)).nextDouble();
    }

    @Test
    public void testZeroVolatilityReturnsDrift() {
        double drift = 1.5;
        double volatility = 0.0;

        BrownianMotionDistribution distribution = new BrownianMotionDistribution();
        distribution.setDrift(drift);
        distribution.setVolatility(volatility);
        distribution.setDt(1);

        IRandomNumberGenerator rng = mock(IRandomNumberGenerator.class);
        when(rng.nextDouble()).thenReturn(0.3, 0.7);

        double actual = distribution.sample(rng);
        assertEquals(drift, actual, 1e-9, "When volatility is zero, the output should equal the drift value");
    }

    @Test
    public void testEdgeCaseNearZero() {
        double drift = 1.0;
        double volatility = 2.0;

        BrownianMotionDistribution distribution = new BrownianMotionDistribution();
        distribution.setDrift(drift);
        distribution.setVolatility(volatility);
        distribution.setDt(1);

        IRandomNumberGenerator rng = mock(IRandomNumberGenerator.class);

        double u1 = 1e-10;
        double u2 = 0.5;

        when(rng.nextDouble()).thenReturn(u1, u2);

        double standardNormal = Math.sqrt(-2 * Math.log(u1)) * Math.cos(2 * Math.PI * u2);
        double expected = (drift - 0.5 * volatility * volatility) + volatility * standardNormal;
        double actual = distribution.sample(rng);

        assertEquals(expected, actual, 1e-9, "The sample method should handle a u1 value very close to zero correctly.");
    }

    @Test
    public void testConsistencyWithDeterministicRNG() {
        double drift = 1.0;
        double volatility = 2.0;
        BrownianMotionDistribution distribution = new BrownianMotionDistribution();
        distribution.setDrift(drift);
        distribution.setVolatility(volatility);
        distribution.setDt(1);

        IRandomNumberGenerator rng = mock(IRandomNumberGenerator.class);
        when(rng.nextDouble()).thenReturn(0.5, 0.25, 0.7, 0.3, 0.2, 0.8);

        double expected1 = (drift - 0.5 * volatility * volatility) + volatility * (Math.sqrt(-2 * Math.log(0.5)) * Math.cos(2 * Math.PI * 0.25));
        double expected2 = (drift - 0.5 * volatility * volatility) + volatility * (Math.sqrt(-2 * Math.log(0.7)) * Math.cos(2 * Math.PI * 0.3));
        double expected3 = (drift - 0.5 * volatility * volatility) + volatility * (Math.sqrt(-2 * Math.log(0.2)) * Math.cos(2 * Math.PI * 0.8));

        double actual1 = distribution.sample(rng);
        double actual2 = distribution.sample(rng);
        double actual3 = distribution.sample(rng);

        assertEquals(expected1, actual1, 1e-9, "The first sample should match the expected value based on the known RNG values.");
        assertEquals(expected2, actual2, 1e-9, "The second sample should match the expected value based on the known RNG values.");
        assertEquals(expected3, actual3, 1e-9, "The third sample should match the expected value based on the known RNG values.");
    }
}
