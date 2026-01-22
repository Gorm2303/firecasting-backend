package dk.gormkrings.dto;

import dk.gormkrings.annotations.UIField;
import dk.gormkrings.returns.ReturnerConfig;
import dk.gormkrings.simulation.data.Date;
import dk.gormkrings.tax.TaxExemptionConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Advanced simulation request.
 *
 * This endpoint is intended to evolve as advanced configuration expands (returner, inflation,
 * distribution params, etc.) while keeping the legacy SimulationRequest stable.
 */
@Setter
@Getter
public class AdvancedSimulationRequest {

    @UIField(label = "Start Date", type = "date", required = true)
    @NotNull
    private Date startDate;

    @Valid
    @NotEmpty
    private List<@Valid PhaseRequest> phases;

    @UIField(label = "Overall Tax Rule", type = "dropdown", options = {"Capital", "Notional"}, required = true)
    @NotBlank
    private String overallTaxRule;

    @UIField(label = "Tax Percentage", type = "number", required = true)
    private float taxPercentage;

    /** Returner type key understood by DefaultReturnFactory (e.g. simpleReturn, dataDrivenReturn, distributionReturn). */
    private String returnType;

    /** Optional returner configuration for advanced-mode (seed, distribution parameters, etc.). */
    @Valid
    private ReturnerConfig returnerConfig;

    /**
     * Optional RNG seed for the Monte Carlo run.
     * If set, this is applied to the returner RNG stream (and regime transitions).
     * Negative values force stochastic behavior.
     */
    @UIField(label = "RNG Seed", type = "number")
    private Long seed;

    /** Optional tax exemption overrides used when phases include tax rules (exemptioncard/stockexemption). */
    @Valid
    private TaxExemptionConfig taxExemptionConfig;

    /**
     * Inflation factor. The backend spec expects a factor (e.g. 1.02 = 2% yearly).
     * The frontend may send a percentage; if so, the mapping layer should convert.
     */
    private double inflationFactor;

    /**
     * Yearly fee percentage applied to current capital at year-end (e.g. 0.5 = 0.5% per year).
     * Missing/negative values default to 0.
     */
    private double yearlyFeePercentage;
}
