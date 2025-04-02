import dk.gormkrings.math.randomNumberGenerator.IRandomNumberGenerator;
import dk.gormkrings.randomNumberGenerator.DefaultRandomNumberGenerator;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.AssertionErrors.assertTrue;
import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest;

import java.util.ArrayList;
import java.util.List;

public class DefaultRandomNumberGeneratorTest {

    @Test
    public void testNextDoubleWithinRange() {
        DefaultRandomNumberGenerator generator = new DefaultRandomNumberGenerator(123L);
        for (int i = 0; i < 100; i++) {
            double value = generator.nextDouble();
            assertTrue("Value should be >= 0.0", value >= 0.0);
            assertTrue("Value should be < 1.0", value < 1.0);
        }
    }

    @Test
    public void testSeedConsistency() {
        long seed = 123L;
        DefaultRandomNumberGenerator generator1 = new DefaultRandomNumberGenerator(seed);
        DefaultRandomNumberGenerator generator2 = new DefaultRandomNumberGenerator(seed);

        for (int i = 0; i < 10; i++) {
            double value1 = generator1.nextDouble();
            double value2 = generator2.nextDouble();
            assertEquals(value1, value2, 1e-9, "Sequences should be identical for same seed");
        }
    }

    @Test
    public void testUniformDistributionAggregate() {
        int failingTests = 0;
        int totalTests = 1000;
        int sampleSize = 10000;
        int numBuckets = 10;
        double criticalValue = 16.919;

        for (int n = 0; n < totalTests; n++) {
            DefaultRandomNumberGenerator generator = new DefaultRandomNumberGenerator(n);
            int[] counts = new int[numBuckets];

            for (int i = 0; i < sampleSize; i++) {
                double value = generator.nextDouble();
                int bucket = (int) (value * numBuckets);
                if (bucket == numBuckets) {
                    bucket = numBuckets - 1;
                }
                counts[bucket]++;
            }

            double expectedCount = sampleSize / (double) numBuckets;
            double chiSquare = 0.0;

            for (int count : counts) {
                chiSquare += Math.pow(count - expectedCount, 2) / expectedCount;
            }

            if (chiSquare >= criticalValue) {
                failingTests++;
            }
        }

        double failureRate = (double) failingTests / totalTests;
        System.out.println("Failure rate: " + failureRate);
        assertTrue("Too many seeds failed the uniformity test", failureRate < 0.08);
    }

    @Test
    public void testKSTestForUniformity() {
        DefaultRandomNumberGenerator generator = new DefaultRandomNumberGenerator(123L);
        int sampleSize = 100000;
        double[] samples = new double[sampleSize];

        for (int i = 0; i < sampleSize; i++) {
            samples[i] = generator.nextDouble();
        }

        UniformRealDistribution uniform = new UniformRealDistribution(0.0, 1.0);
        KolmogorovSmirnovTest ksTest = new KolmogorovSmirnovTest();

        double pValue = ksTest.kolmogorovSmirnovTest(uniform, samples);

        assertTrue("KS test indicates a non-uniform distribution", pValue > 0.05);
    }

    private int calculateRuns(double[] numbers, double threshold) {
        if (numbers.length == 0) {
            return 0;
        }
        int runs = 1;
        boolean previousAbove = numbers[0] >= threshold;
        for (int i = 1; i < numbers.length; i++) {
            boolean currentAbove = numbers[i] >= threshold;
            if (currentAbove != previousAbove) {
                runs++;
                previousAbove = currentAbove;
            }
        }
        return runs;
    }

    @Test
    public void testRunsTestWithMockedSequence() {
        IRandomNumberGenerator rngMock = mock(IRandomNumberGenerator.class);

        when(rngMock.nextDouble()).thenReturn(0.6, 0.7, 0.2, 0.1, 0.8, 0.9);

        int sequenceLength = 6;
        double[] generatedValues = new double[sequenceLength];
        for (int i = 0; i < sequenceLength; i++) {
            generatedValues[i] = rngMock.nextDouble();
        }

        double threshold = 0.5;
        int runs = calculateRuns(generatedValues, threshold);

        assertEquals(3, runs, "The number of runs should be 3");

        verify(rngMock, times(6)).nextDouble();
    }

    private double autocorrelation(double[] data, int lag) {
        int n = data.length;
        double mean = 0.0;
        for (double d : data) {
            mean += d;
        }
        mean /= n;

        double numerator = 0.0;
        double denominator = 0.0;
        for (double datum : data) {
            double diff = datum - mean;
            denominator += diff * diff;
        }
        for (int i = 0; i < n - lag; i++) {
            numerator += (data[i] - mean) * (data[i + lag] - mean);
        }
        return numerator / denominator;
    }

    @Test
    public void testAutocorrelationForIndependence() {
        DefaultRandomNumberGenerator generator = new DefaultRandomNumberGenerator(123L);
        IRandomNumberGenerator spyGenerator = spy(generator);

        int sampleSize = 100000;
        double[] samples = new double[sampleSize];

        for (int i = 0; i < sampleSize; i++) {
            samples[i] = spyGenerator.nextDouble();
        }

        for (int lag = 1; lag <= 5; lag++) {
            double ac = autocorrelation(samples, lag);
            System.out.println("Lag " + lag + " autocorrelation: " + ac);
            assertTrue("Autocorrelation at lag " + lag + " is too high: " + ac,
                    Math.abs(ac) < 0.05);
        }

        verify(spyGenerator, times(sampleSize)).nextDouble();
    }

    @Test
    public void testSerialPairDistribution() {
        DefaultRandomNumberGenerator generator = new DefaultRandomNumberGenerator(123L);
        IRandomNumberGenerator spyGenerator = spy(generator);

        int samplePairs = 100000;
        int numBuckets = 10;
        int[][] gridCounts = new int[numBuckets][numBuckets];

        for (int i = 0; i < samplePairs; i++) {
            double first = spyGenerator.nextDouble();
            double second = spyGenerator.nextDouble();

            int row = (int) (first * numBuckets);
            int col = (int) (second * numBuckets);

            if (row == numBuckets) {
                row = numBuckets - 1;
            }
            if (col == numBuckets) {
                col = numBuckets - 1;
            }
            gridCounts[row][col]++;
        }

        double expectedCount = (double) samplePairs / (numBuckets * numBuckets);
        double chiSquare = 0.0;

        for (int i = 0; i < numBuckets; i++) {
            for (int j = 0; j < numBuckets; j++) {
                double observed = gridCounts[i][j];
                chiSquare += Math.pow(observed - expectedCount, 2) / expectedCount;
            }
        }

        double criticalValue = 123.225;
        System.out.println("Chi-Square statistic: " + chiSquare);
        assertTrue("Serial test indicates non-uniform distribution of pairs. Chi-square = " + chiSquare,
                chiSquare < criticalValue);
        verify(spyGenerator, times(2 * samplePairs)).nextDouble();
    }

    @Test
    public void testGapDistribution() {
        DefaultRandomNumberGenerator generator = new DefaultRandomNumberGenerator(123L);
        IRandomNumberGenerator spyGenerator = spy(generator);

        double subIntervalLower = 0.0;
        double subIntervalUpper = 0.2;
        double p = subIntervalUpper - subIntervalLower; // p = 0.2

        int totalSamples = 100000;
        List<Integer> gaps = new ArrayList<>();
        int currentGap = 0;

        for (int i = 0; i < totalSamples; i++) {
            double value = spyGenerator.nextDouble();
            if (value >= subIntervalLower && value < subIntervalUpper) {
                gaps.add(currentGap);
                currentGap = 0;
            } else {
                currentGap++;
            }
        }

        double sumGaps = 0.0;
        for (int gap : gaps) {
            sumGaps += gap;
        }
        double averageGap = gaps.isEmpty() ? 0 : sumGaps / gaps.size();
        double expectedMeanGap = (1.0 - p) / p;

        System.out.println("Average gap length: " + averageGap + ", expected: " + expectedMeanGap);
        assertTrue("Average gap length is too far from the theoretical value. Expected near " +
                expectedMeanGap + " but got " + averageGap, Math.abs(averageGap - expectedMeanGap) < 0.5);
        verify(spyGenerator, times(totalSamples)).nextDouble();
    }
}
