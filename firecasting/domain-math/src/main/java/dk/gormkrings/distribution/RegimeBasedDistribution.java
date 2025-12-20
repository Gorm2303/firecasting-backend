package dk.gormkrings.distribution;

import dk.gormkrings.math.distribution.IDistribution;
import dk.gormkrings.math.randomNumberGenerator.IRandomNumberGenerator;
import dk.gormkrings.regime.IRegimeProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Component("regimeBased")
@Scope("prototype")
public class RegimeBasedDistribution implements IDistribution {
    private final IDistribution[] regimeDistributions;
    private final IRegimeProvider regimeProvider;
    private static boolean initialized = false;

    /**
     * Constructs a RegimeBasedDistribution.
     *
     * @param regimeDistributions an array of IDistribution instances, each for a different regime.
     * @param regimeProvider      an IRegimeProvider instance that provides the current regime index.
     */
    public RegimeBasedDistribution(IDistribution[] regimeDistributions, IRegimeProvider regimeProvider) {
        this.regimeDistributions = regimeDistributions;
        this.regimeProvider = regimeProvider;
        if (!initialized) {
            log.info("Creating new Regime-Based Distribution: {}", Arrays.toString(regimeDistributions));
            initialized = true;
        }
    }

    @Override
    public double sample(IRandomNumberGenerator rng) {
        int currentRegime = regimeProvider.getCurrentRegime();
        // Ensure currentRegime is within bounds
        if (currentRegime < 0 || currentRegime >= regimeDistributions.length) {
            throw new IllegalArgumentException("Invalid regime index: " + currentRegime);
        }
        return regimeDistributions[currentRegime].sample(rng);
    }

    /**
     * Advance the regime provider at the end of a month.
     *
     * <p>This allows the simulation to keep daily returns while switching regimes monthly.
     * Providers that do not implement month-based switching will simply ignore this hook.</p>
     */
    public void onMonthEnd() {
        regimeProvider.onMonthEnd();
    }

    @Override
    public IDistribution copy() {
        // Create copies for each regime distribution.
        IDistribution[] newRegimes = new IDistribution[regimeDistributions.length];
        for (int i = 0; i < regimeDistributions.length; i++) {
            newRegimes[i] = regimeDistributions[i].copy();
        }
        // For simplicity, we reuse the same regime provider instance.
        return new RegimeBasedDistribution(newRegimes, regimeProvider.copy());
    }

    @Override
    public String toString() {
        return "RegimeBasedDistribution{" +
                "regimeDistributions=" + Arrays.toString(regimeDistributions) +
                ", regimeProvider=" + regimeProvider +
                '}';
    }
}
