package dk.gormkrings.dto;

import dk.gormkrings.simulation.data.Date;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class SimulationRequest {
    private Date startDate;
    // The phases are given in order.
    private List<PhaseRequest> phases;

    // Specification parameters
    private String overallTaxRule;
    private float taxPercentage;
    private float returnPercentage;

    public int getEpochDay() {
        return startDate.getEpochDay();
    }
}
