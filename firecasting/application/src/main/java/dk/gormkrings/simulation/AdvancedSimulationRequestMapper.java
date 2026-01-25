package dk.gormkrings.simulation;

import dk.gormkrings.dto.AdvancedSimulationRequest;

/**
 * Maps AdvancedSimulationRequest to the internal SimulationRunSpec.
 */
public final class AdvancedSimulationRequestMapper {

    // Must match FirecastingController.DEFAULT_MASTER_SEED (kept duplicated to avoid cross-module coupling).
    private static final long DEFAULT_MASTER_SEED = 1L;

    private AdvancedSimulationRequestMapper() {
    }

    public static SimulationRunSpec toRunSpec(AdvancedSimulationRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("request must not be null");
        }

        // Defensive defaults: keep behavior predictable even if client omits values.
        String returnType = (req.getReturnType() == null || req.getReturnType().isBlank())
                ? "dataDrivenReturn"
                : req.getReturnType();

        double inflationFactor = req.getInflationFactor();
        if (inflationFactor <= 0.0) {
            // Default to 1.02 to match legacy behavior.
            inflationFactor = 1.02D;
        }

        double yearlyFeePercentage = req.getYearlyFeePercentage();
        if (!Double.isFinite(yearlyFeePercentage) || yearlyFeePercentage < 0.0) {
            yearlyFeePercentage = 0.0;
        }

        // Normalize seed: null/negative => generate positive seed.
        Long requestedSeed = req.getSeed();
        if (requestedSeed == null && req.getReturnerConfig() != null) {
            requestedSeed = req.getReturnerConfig().getSeed();
        }

        long normalizedSeed = normalizeSeed(requestedSeed);
        req.setSeed(normalizedSeed);

        if (req.getReturnerConfig() == null) {
            var cfg = new dk.gormkrings.returns.ReturnerConfig();
            cfg.setSeed(normalizedSeed);
            req.setReturnerConfig(cfg);
        } else {
            req.getReturnerConfig().setSeed(normalizedSeed);
        }

        return new SimulationRunSpec(
                req.getStartDate(),
                req.getPhases(),
                req.getOverallTaxRule(),
                req.getTaxPercentage(),
                returnType,
                inflationFactor,
                yearlyFeePercentage,
                req.getReturnerConfig(),
                req.getTaxExemptionConfig()
        );
    }

    private static long normalizeSeed(Long seed) {
        // Contract:
        // - null => deterministic default seed (deduplicates)
        // - negative => random positive seed (stochastic)
        if (seed == null) return DEFAULT_MASTER_SEED;
        if (seed < 0) {
            return java.util.concurrent.ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
        }
        return seed;
    }
}
