package dk.gormkrings.dto;

import dk.gormkrings.annotations.UIField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotNull;

@Getter
@Setter
public class PhaseRequest {

    @UIField(label = "Phase Type", type = "dropdown", options = {"DEPOSIT", "PASSIVE", "WITHDRAW"}, required = true)
        @Schema(
            description = "Phase type.",
            example = "DEPOSIT"
        )
    @NotNull
    private PhaseType phaseType;

    @UIField(label = "Duration (months)", type = "number", required = true)
    private int durationInMonths;

    @UIField(label = "Initial Deposit", type = "number")
    private Double initialDeposit;

    @UIField(label = "Monthly Deposit", type = "number")
    private Double monthlyDeposit;

    @UIField(label = "Annual Increase %", type = "number")
    private Double yearlyIncreaseInPercentage;

    @UIField(label = "Withdraw Rate", type = "number")
    private Double withdrawRate;

    @UIField(label = "Withdraw Amount", type = "number")
    private Double withdrawAmount;

    @UIField(label = "Lower Variation %", type = "number")
    private Double lowerVariationPercentage;

    @UIField(label = "Upper Variation %", type = "number")
    private Double upperVariationPercentage;

    @UIField(label = "Tax Rules", type = "text") // Could be an array widget
    @Schema(
        description = "Optional tax exemption rules applied in this phase (factory keys).")
    private TaxExemptionRule[] taxRules;
}

