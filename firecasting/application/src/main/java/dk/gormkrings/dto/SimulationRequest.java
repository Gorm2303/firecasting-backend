package dk.gormkrings.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dk.gormkrings.annotations.UIField;
import dk.gormkrings.simulation.data.Date;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.validation.constraints.AssertTrue;
import java.util.List;
import java.util.Objects;

@Setter
@Getter
public class SimulationRequest {

    @UIField(label = "Start Date", type = "date", required = true)
    @NotNull
    private Date startDate;

    @Valid
    @NotEmpty
    private List<@Valid PhaseRequest> phases;

    @UIField(label = "Overall Tax Rule", type = "dropdown", options = {"Capital", "Notional"}, required = true)
        @Schema(
            description = "Overall tax rule used by the simulation.",
            example = "Capital"
        )
    @NotNull
    private OverallTaxRule overallTaxRule;

    @UIField(label = "Tax Percentage", type = "number", required = true)
    private float taxPercentage;

    @UIField(label = "Return Percentage", type = "number", required = true)
    private float returnPercentage;

    @UIField(label = "Master seed", type = "number")
    private Long seed;

    @JsonIgnore
    public int getEpochDay() {
        return startDate.getEpochDay();
    }

    /** Cross-field rule: total months across all phases must be ≤ 1200. */
    @AssertTrue(message = "Total duration across phases must be ≤ 1200 months")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isTotalDurationValid() {
        if (phases == null) return false;                 // guarded by @NotEmpty, but explicit
        int total = phases.stream()
                .filter(Objects::nonNull)
                .mapToInt(p -> Math.max(0, p.getDurationInMonths()))
                .sum();
        return total <= 1200;
    }
}
