package dk.gormkrings.simulation;

import dk.gormkrings.dto.AdvancedSimulationRequest;

/**
 * Maps AdvancedSimulationRequest to the internal SimulationRunSpec.
 */
public final class AdvancedSimulationRequestMapper {

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

        // Allow a top-level seed to drive seeding even when returnerConfig is missing.
        // Prefer explicit top-level seed over nested returnerConfig.seed.
        if (req.getSeed() != null) {
            if (req.getReturnerConfig() == null) {
                var cfg = new dk.gormkrings.returns.ReturnerConfig();
                cfg.setSeed(req.getSeed());
                req.setReturnerConfig(cfg);
            } else if (req.getReturnerConfig().getSeed() == null) {
                req.getReturnerConfig().setSeed(req.getSeed());
            }
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
}
