package dk.gormkrings.simulation;

import dk.gormkrings.dto.PhaseRequest;
import dk.gormkrings.simulation.data.Date;
import dk.gormkrings.returns.ReturnerConfig;
import dk.gormkrings.tax.TaxExemptionConfig;

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

    /** Yearly fee percentage applied to capital at year-end (e.g. 0.5 = 0.5%). */
    private final double yearlyFeePercentage;

    /** Optional returner configuration (advanced-mode). */
    private final ReturnerConfig returnerConfig;

    /** Optional tax exemption overrides (advanced-mode). */
    private final TaxExemptionConfig taxExemptionConfig;

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
        this.yearlyFeePercentage = 0.0;
        this.returnerConfig = null;
        this.taxExemptionConfig = null;
    }

    public SimulationRunSpec(
            Date startDate,
            List<PhaseRequest> phases,
            String overallTaxRule,
            float taxPercentage,
            String returnType,
            double inflationFactor,
            double yearlyFeePercentage,
            ReturnerConfig returnerConfig,
            TaxExemptionConfig taxExemptionConfig) {
        this.startDate = startDate;
        this.phases = phases;
        this.overallTaxRule = overallTaxRule;
        this.taxPercentage = taxPercentage;
        this.returnType = returnType;
        this.inflationFactor = inflationFactor;
        this.yearlyFeePercentage = yearlyFeePercentage;
        this.returnerConfig = returnerConfig;
        this.taxExemptionConfig = taxExemptionConfig;
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

    public double getYearlyFeePercentage() {
        return yearlyFeePercentage;
    }

    public ReturnerConfig getReturnerConfig() {
        return returnerConfig;
    }

    public TaxExemptionConfig getTaxExemptionConfig() {
        return taxExemptionConfig;
    }

    public int getTotalMonths() {
        if (phases == null) return 0;
        return phases.stream()
                .filter(Objects::nonNull)
                .mapToInt(p -> Math.max(0, p.getDurationInMonths()))
                .sum();
    }
}
