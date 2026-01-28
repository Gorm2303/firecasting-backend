package dk.gormkrings.diff;

import lombok.Data;

@Data
public class RunDiffResponse {

    private RunInfo a;
    private RunInfo b;

    private Attribution attribution;
    private RunDiffComparator.ComparisonResult output;

    @Data
    public static class RunInfo {
        private String id;
        /** ISO-8601 string (avoid requiring Jackson JavaTime module registration). */
        private String createdAt;
        private String inputHash;
        private Long rngSeed;
        /** String form to avoid JS number precision loss in clients. */
        private String rngSeedText;
        private String modelAppVersion;
        private String modelBuildTime;
        private String modelSpringBootVersion;
        private String modelJavaVersion;
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
