package dk.gormkrings.simulation;

import dk.gormkrings.dto.PhaseRequest;
import dk.gormkrings.simulation.data.Date;
import dk.gormkrings.returns.ReturnerConfig;

import java.util.List;
import java.util.Objects;

/**
 * Internal run specification used to execute a simulation.
 *
 * This is intentionally DTO-agnostic so multiple API endpoints can map into the same
 * execution configuration without duplicating the simulation pipeline.
 */
public final class SimulationRunSpec {

    private final Date startDate;
    private final List<PhaseRequest> phases;

    /** Overall tax rule name as expected by {@code ITaxRuleFactory}. */
    private final String overallTaxRule;

    /** Tax percentage used by {@code ITaxRuleFactory}. */
    private final float taxPercentage;

    /** Returner type key as expected by {@code IReturnFactory}. */
    private final String returnType;

    /** Inflation factor used by {@code IInflationFactory} (e.g. 1.02 for 2%). */
    private final double inflationFactor;

    /** Optional returner configuration (advanced-mode). */
    private final ReturnerConfig returnerConfig;

    public SimulationRunSpec(
            Date startDate,
            List<PhaseRequest> phases,
            String overallTaxRule,
            float taxPercentage,
            String returnType,
            double inflationFactor) {
        this.startDate = startDate;
        this.phases = phases;
        this.overallTaxRule = overallTaxRule;
        this.taxPercentage = taxPercentage;
        this.returnType = returnType;
        this.inflationFactor = inflationFactor;
        this.returnerConfig = null;
    }

    public SimulationRunSpec(
            Date startDate,
            List<PhaseRequest> phases,
            String overallTaxRule,
            float taxPercentage,
            String returnType,
            double inflationFactor,
            ReturnerConfig returnerConfig) {
        this.startDate = startDate;
        this.phases = phases;
        this.overallTaxRule = overallTaxRule;
        this.taxPercentage = taxPercentage;
        this.returnType = returnType;
        this.inflationFactor = inflationFactor;
        this.returnerConfig = returnerConfig;
    }

    public Date getStartDate() {
        return startDate;
    }

    public int getEpochDay() {
        return startDate.getEpochDay();
    }

    public List<PhaseRequest> getPhases() {
        return phases;
    }

    public String getOverallTaxRule() {
        return overallTaxRule;
    }

    public float getTaxPercentage() {
        return taxPercentage;
    }

    public String getReturnType() {
        return returnType;
    }

    public double getInflationFactor() {
        return inflationFactor;
    }

    public ReturnerConfig getReturnerConfig() {
        return returnerConfig;
    }

    public int getTotalMonths() {
        if (phases == null) return 0;
        return phases.stream()
                .filter(Objects::nonNull)
                .mapToInt(p -> Math.max(0, p.getDurationInMonths()))
                .sum();
    }
}
