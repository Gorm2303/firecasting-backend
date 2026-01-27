package dk.gormkrings.dto;

import dk.gormkrings.annotations.UIField;
import dk.gormkrings.returns.ReturnerConfig;
import dk.gormkrings.simulation.data.Date;
import dk.gormkrings.tax.TaxExemptionConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    /**
     * Optional override for how many Monte Carlo paths/runs to execute.
     * If missing, the backend uses its configured default.
     */
    @UIField(label = "Paths (runs)", type = "number")
    @Min(1)
    @Max(100000)
    private Integer paths;

    /**
     * Optional override for the engine batch size.
     * If missing, the backend uses its configured default.
     */
    @UIField(label = "Batch size", type = "number")
    @Min(1)
    @Max(100000)
    private Integer batchSize;

    @UIField(label = "Start Date", type = "date", required = true)
    @NotNull
    private Date startDate;

    @Valid
    @NotEmpty
    private List<@Valid PhaseRequest> phases;

    @UIField(label = "Overall Tax Rule", type = "dropdown", options = {"Capital", "Notional"}, required = true)
    @NotNull
    private OverallTaxRule overallTaxRule;

    @UIField(label = "Tax Percentage", type = "number", required = true)
    private float taxPercentage;

    /** Returner type key understood by DefaultReturnFactory (e.g. simpleReturn, dataDrivenReturn, distributionReturn). */
    private String returnType;

    /** Optional returner configuration for advanced-mode (seed, distribution parameters, etc.). */
    @Valid
    private ReturnerConfig returnerConfig;

    /**
     * Master seed for the Monte Carlo run.
     *
     * Contract:
     *  - null => use default deterministic seed (deduplicates)
     *  - negative => random seed (new positive seed each run; not persisted)
     *  - positive => deterministic custom seed (deduplicates)
     */
    @UIField(label = "Master seed", type = "number")
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
