package dk.gormkrings.diff;

import lombok.Data;

@Data
public class RunDetailsDto {
    private String id;
    /** ISO-8601 string (avoid requiring Jackson JavaTime module registration). */
    private String createdAt;

    private Long rngSeed;

    private String modelAppVersion;
    private String modelBuildTime;
    private String modelSpringBootVersion;
    private String modelJavaVersion;

    private String inputHash;

    // --- Timing breakdown (milliseconds) ---
    private Long computeMs;
    private Long aggregateMs;
    private Long gridsMs;
    private Long persistMs;
    private Long totalMs;
}
