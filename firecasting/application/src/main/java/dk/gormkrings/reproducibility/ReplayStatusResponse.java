package dk.gormkrings.reproducibility;

import lombok.Data;

@Data
public class ReplayStatusResponse {
    private String replayId;
    private String status;
    private String simulationId;
    private boolean exactMatch;
    private boolean withinTolerance;
    private int mismatches;
    private double maxAbsDiff;
    private String note;
}
