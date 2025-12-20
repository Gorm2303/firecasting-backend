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

        return new SimulationRunSpec(
                req.getStartDate(),
                req.getPhases(),
                req.getOverallTaxRule(),
                req.getTaxPercentage(),
                returnType,
            inflationFactor,
            req.getReturnerConfig()
        );
    }
}
