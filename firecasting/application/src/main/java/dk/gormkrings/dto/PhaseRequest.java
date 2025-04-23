package dk.gormkrings.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PhaseRequest {
    // Getters and setters
    // Use an enum in a real system; here we use String for simplicity.
    private String phaseType; // "DEPOSIT", "PASSIVE", "WITHDRAW"
    private int durationInMonths;
    // Only for deposit phases:
    private Double initialDeposit;
    private Double monthlyDeposit;
    private Double yearlyIncreaseInPercentage;
    // Only for withdraw phases:
    private Double withdrawRate;
    private Double withdrawAmount;
    private Double lowerVariationPercentage;
    private Double upperVariationPercentage;
}

