package dk.gormkrings.returns;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration for creating returners. Used by advanced-mode requests.
 */
@Getter
@Setter
public class ReturnerConfig {

    /**
     * If negative, the RNG should be stochastic. If non-negative, it should be deterministic.
     */
    private Long seed;

    /**
     * Simple return config (annual percentage).
     * Example: 7 means 7%.
     */
    private Double simpleAveragePercentage;

    /**
     * Distribution return config.
     */
    private DistributionConfig distribution;

    @Getter
    @Setter
    public static class DistributionConfig {
        /** Supported values: normal, brownianMotion, studentT, regimeBased */
        private String type;

        private NormalParams normal;
        private BrownianParams brownianMotion;
        private StudentTParams studentT;

        /** Regime-based distribution config (v1). */
        private RegimeBasedParams regimeBased;
    }

    @Getter
    @Setter
    public static class RegimeBasedParams {
        /** Time step in months (v1 expects 1). If null, defaults to 1. */
        private Integer tickMonths;

        /** Regime definitions. v1 expects 3 regimes (index 0..2). */
        private java.util.List<RegimeParams> regimes;
    }

    @Getter
    @Setter
    public static class RegimeParams {
        /** Supported values (v1): normal, studentT */
        private String distributionType;

        /** Annualized params for NormalDistribution (used when distributionType=normal). */
        private NormalParams normal;

        /** Annualized params for TDistributionImpl (used when distributionType=studentT). */
        private StudentTParams studentT;

        /** Expected time spent in this regime (months). Used to derive switch probability per tick. */
        private Double expectedDurationMonths;

        /** Switch target weights when leaving this regime (v1 expects weights to the other two regimes). */
        private SwitchWeights switchWeights;
    }

    @Getter
    @Setter
    public static class SwitchWeights {
        private Double toRegime0;
        private Double toRegime1;
        private Double toRegime2;
    }

    @Getter
    @Setter
    public static class NormalParams {
        /** Annualized mean (e.g. 0.07). */
        private Double mean;
        /** Annualized standard deviation (e.g. 0.20). */
        private Double standardDeviation;
    }

    @Getter
    @Setter
    public static class BrownianParams {
        private Double drift;
        private Double volatility;
    }

    @Getter
    @Setter
    public static class StudentTParams {
        private Double mu;
        private Double sigma;
        private Double nu;
    }
}