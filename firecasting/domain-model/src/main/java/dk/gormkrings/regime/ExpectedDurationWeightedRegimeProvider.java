package dk.gormkrings.regime;

import java.util.SplittableRandom;

/**
 * A simple regime provider intended for advanced-mode (v1) regime configuration.
 *
 * Model:
 * - Each tick, with probability pSwitch = 1 / expectedDurationMonths[current], we switch regimes.
 * - If switching, we choose the next regime based on configured weights to each target regime.
 *
 * Notes:
 * - This provider owns its own RNG stream (SplittableRandom) so that regime transitions can be deterministic
 *   given a seed, while remaining independent from return sampling RNG.
 */
public final class ExpectedDurationWeightedRegimeProvider implements IRegimeProvider {

    private final double[] expectedDurationMonths;
    private final double[][] switchWeights;
    private final SplittableRandom random;

    private int currentRegime;

    public ExpectedDurationWeightedRegimeProvider(
            int initialRegime,
            double[] expectedDurationMonths,
            double[][] switchWeights,
            Long seed
    ) {
        if (expectedDurationMonths == null || expectedDurationMonths.length == 0) {
            throw new IllegalArgumentException("expectedDurationMonths must be non-empty");
        }
        int n = expectedDurationMonths.length;

        if (switchWeights == null || switchWeights.length != n) {
            throw new IllegalArgumentException("switchWeights must be an n x n matrix");
        }
        for (int i = 0; i < n; i++) {
            if (switchWeights[i] == null || switchWeights[i].length != n) {
                throw new IllegalArgumentException("switchWeights must be an n x n matrix");
            }
        }

        this.expectedDurationMonths = expectedDurationMonths.clone();

        this.switchWeights = new double[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(switchWeights[i], 0, this.switchWeights[i], 0, n);
        }

        this.currentRegime = clampIndex(initialRegime, n);
        this.random = (seed == null) ? new SplittableRandom() : new SplittableRandom(seed);
    }

    private ExpectedDurationWeightedRegimeProvider(
            int currentRegime,
            double[] expectedDurationMonths,
            double[][] switchWeights,
            SplittableRandom random
    ) {
        this.currentRegime = currentRegime;
        this.expectedDurationMonths = expectedDurationMonths;
        this.switchWeights = switchWeights;
        this.random = random;
    }

    @Override
    public int getCurrentRegime() {
        int n = expectedDurationMonths.length;

        double duration = expectedDurationMonths[currentRegime];
        double pSwitch = (duration <= 0.0) ? 1.0 : (1.0 / duration);
        pSwitch = clamp01(pSwitch);

        if (random.nextDouble() >= pSwitch) {
            return currentRegime;
        }

        int next = sampleNextRegime(currentRegime);
        currentRegime = next;
        return currentRegime;
    }

    private int sampleNextRegime(int from) {
        int n = expectedDurationMonths.length;

        // Weights row; ignore self-transition even if configured.
        double total = 0.0;
        for (int j = 0; j < n; j++) {
            if (j == from) continue;
            double w = switchWeights[from][j];
            if (w > 0.0) total += w;
        }

        // If no weights given, fall back to uniform among other regimes.
        if (total <= 0.0) {
            int offset = random.nextInt(n - 1);
            return (offset >= from) ? offset + 1 : offset;
        }

        double r = random.nextDouble() * total;
        double cumulative = 0.0;
        for (int j = 0; j < n; j++) {
            if (j == from) continue;
            double w = switchWeights[from][j];
            if (w <= 0.0) continue;
            cumulative += w;
            if (r < cumulative) {
                return j;
            }
        }

        // Numerical edge-case: fall back to last valid.
        for (int j = n - 1; j >= 0; j--) {
            if (j != from && switchWeights[from][j] > 0.0) return j;
        }
        // Should never happen due to total > 0.
        return (from == 0) ? 1 : 0;
    }

    @Override
    public IRegimeProvider copy() {
        int n = expectedDurationMonths.length;

        double[] durationsCopy = expectedDurationMonths.clone();
        double[][] weightsCopy = new double[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(switchWeights[i], 0, weightsCopy[i], 0, n);
        }

        return new ExpectedDurationWeightedRegimeProvider(currentRegime, durationsCopy, weightsCopy, random.split());
    }

    private static int clampIndex(int idx, int n) {
        if (idx < 0) return 0;
        if (idx >= n) return n - 1;
        return idx;
    }

    private static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }
}
