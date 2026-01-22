package dk.gormkrings.diff;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class RunDiffResponse {

    private RunInfo a;
    private RunInfo b;

    private Attribution attribution;
    private RunDiffComparator.ComparisonResult output;

    @Data
    public static class RunInfo {
        private String id;
        private OffsetDateTime createdAt;
        private String inputHash;
        private Long rngSeed;
        private String modelAppVersion;
    }

    @Data
    public static class Attribution {
        /** True when non-random input parameters differ (tax rules, phases, etc.). */
        private boolean inputsChanged;
        /** True when the RNG-related inputs differ (seed or stochastic mode). */
        private boolean randomnessChanged;
        /** True when app/model version differs. */
        private boolean modelVersionChanged;
        /** Human-readable explanation. */
        private String summary;
    }
}
