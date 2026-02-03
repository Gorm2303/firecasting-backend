package dk.gormkrings.export;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

@Data
public class ReproducibilityBundleDto {

    private Meta meta;
    private Inputs inputs;
    /** Optional but recommended: UI timeline context for consistent visualization on import. */
    private Timeline timeline;
    private Outputs outputs;

    @Data
    public static class Meta {
        private String simulationId;
        /** ISO-8601 timestamp (UTC offset included). */
        private String exportedAt;
        /** UI mode as provided by the frontend (normal|advanced). */
        private String uiMode;
        /** Inferred from persisted input payload (normal|advanced). */
        private String inputKind;
        private ModelVersion modelVersion;
    }

    @Data
    public static class ModelVersion {
        /** Application artifact version (from Spring Boot build-info). */
        private String appVersion;
        /** Build time (from Spring Boot build-info). */
        private String buildTime;
        private String springBootVersion;
        private String javaVersion;
    }

    @Data
    public static class Inputs {
        /** Inferred from persisted input payload (normal|advanced). */
        private String kind;
        /** Always included for completeness; equals either normal or advanced payload. */
        private JsonNode raw;
        /** Present when kind==normal; otherwise null. */
        private JsonNode normal;
        /** Present when kind==advanced; otherwise null. */
        private JsonNode advanced;
    }

    @Data
    public static class Timeline {
        /** ISO date string (YYYY-MM-DD). */
        private String startDate;
        /** Phase types in order (DEPOSIT/PASSIVE/WITHDRAW). */
        private List<String> phaseTypes;
        /** Phase durations in months, in the order entered. */
        private List<Integer> phaseDurationsInMonths;
        /** Used as the start anchor for Phase #1 interpolation when no previous year exists. */
        private Double firstPhaseInitialDeposit;
    }

    @Data
    public static class Outputs {
        private List<YearlySummary> yearlySummaries;
    }

    @Data
    public static class YearlySummary {
        private String phaseName;
        private int year;
        private double averageCapital;
        private double medianCapital;
        private double minCapital;
        private double maxCapital;
        private double stdDevCapital;
        private double cumulativeGrowthRate;
        private double quantile5;
        private double quantile25;
        private double quantile75;
        private double quantile95;
        private double var;
        private double cvar;
        private double negativeCapitalPercentage;
    }
}
