package dk.gormkrings.distribution;

import dk.gormkrings.math.distribution.IDistribution;
import dk.gormkrings.math.randomNumberGenerator.IRandomNumberGenerator;
import dk.gormkrings.regime.IRegimeProvider;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class RegimeBasedDistribution implements IDistribution {
    private final IDistribution[] regimeDistributions;
    private final IRegimeProvider regimeProvider;

    /**
     * Constructs a RegimeBasedDistribution.
     *
     * @param regimeDistributions an array of IDistribution instances, each for a different regime.
     * @param regimeProvider      an IRegimeProvider instance that provides the current regime index.
     */
    public RegimeBasedDistribution(IDistribution[] regimeDistributions, IRegimeProvider regimeProvider) {
        this.regimeDistributions = regimeDistributions;
        this.regimeProvider = regimeProvider;
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

    @Override
    public IDistribution copy() {
        // Create copies for each regime distribution.
        IDistribution[] newRegimes = new IDistribution[regimeDistributions.length];
        for (int i = 0; i < regimeDistributions.length; i++) {
            newRegimes[i] = regimeDistributions[i].copy();
        }
        // For simplicity, we reuse the same regime provider instance.
        return new RegimeBasedDistribution(newRegimes, regimeProvider);
    }
}
