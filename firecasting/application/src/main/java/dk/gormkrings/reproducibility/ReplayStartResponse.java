package dk.gormkrings.reproducibility;

import lombok.Data;

@Data
public class ReplayStartResponse {
    private String replayId;
    private String simulationId;
    private String status;
    private String note;
}
