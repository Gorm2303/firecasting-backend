package dk.gormkrings.dto;

import dk.gormkrings.data.IDate;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class SimulationRequest {
    private IDate startDate;
    // The phases are given in order.
    private List<PhaseRequest> phases;

    // Specification parameters
    private float taxPercentage;
    private float returnPercentage;
    // Tax Option
    private String taxRule;

    public int getEpochDay() {
        return startDate.getEpochDay();
    }
}
