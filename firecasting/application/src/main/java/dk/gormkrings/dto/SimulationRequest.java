package dk.gormkrings.dto;

import dk.gormkrings.annotations.UIField;
import dk.gormkrings.simulation.data.Date;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class SimulationRequest {

    @UIField(label = "Start Date", type = "date", required = true)
    private Date startDate;

    // The list of phases will be configured separately
    // You may annotate it if you want to let the frontend add/remove them dynamically.
    // Otherwise, just use one schema per PhaseRequest.
    private List<PhaseRequest> phases;

    @UIField(label = "Overall Tax Rule", type = "dropdown", options = {"Capital", "Notional"}, required = true)
    private String overallTaxRule;

    @UIField(label = "Tax Percentage", type = "number", required = true)
    private float taxPercentage;

    @UIField(label = "Return Percentage", type = "number", required = true)
    private float returnPercentage;

    public int getEpochDay() {
        return startDate.getEpochDay();
    }
}
