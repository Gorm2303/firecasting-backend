package dk.gormkrings.returns;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

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
        @Valid
        private RegimeBasedParams regimeBased;
    }

    @Getter
    @Setter
    public static class RegimeBasedParams {
        /** Time step in months (v1 expects 1). If null, defaults to 1. */
        @Min(value = 1, message = "tickMonths must be 1 (monthly) in v1")
        @Max(value = 1, message = "tickMonths must be 1 (monthly) in v1")
        private Integer tickMonths;

        /** Regime definitions. v1 expects 3 regimes (index 0..2). */
        @NotNull(message = "regimes is required for regimeBased distribution")
        @Size(min = 3, max = 3, message = "regimes must have exactly 3 items (v1)")
        @Valid
        private List<RegimeParams> regimes;

        @AssertTrue(message = "for each regime, at least one switch weight to another regime must be > 0")
        public boolean hasValidOffDiagonalSwitchWeights() {
            if (regimes == null) return true; // let @NotNull handle

            for (int i = 0; i < regimes.size(); i++) {
                RegimeParams r = regimes.get(i);
                if (r == null) return true; // let @Valid / downstream rules handle

                SwitchWeights w = r.getSwitchWeights();
                if (w == null) return true; // let @NotNull handle

                double to0 = w.getToRegime0() == null ? 0.0 : w.getToRegime0();
                double to1 = w.getToRegime1() == null ? 0.0 : w.getToRegime1();
                double to2 = w.getToRegime2() == null ? 0.0 : w.getToRegime2();

                double offDiagSum = switch (i) {
                    case 0 -> to1 + to2;
                    case 1 -> to0 + to2;
                    case 2 -> to0 + to1;
                    default -> to0 + to1 + to2;
                };

                if (!(offDiagSum > 0.0)) {
                    return false;
                }
            }

            return true;
        }
    }

    @Getter
    @Setter
    public static class RegimeParams {
        /** Supported values (v1): normal, studentT */
        @NotBlank(message = "distributionType is required (normal|studentT)")
        private String distributionType;

        /** Annualized params for NormalDistribution (used when distributionType=normal). */
        @Valid
        private NormalParams normal;

        /** Annualized params for TDistributionImpl (used when distributionType=studentT). */
        @Valid
        private StudentTParams studentT;

        /** Expected time spent in this regime (months). Used to derive switch probability per tick. */
        @NotNull(message = "expectedDurationMonths is required")
        @Positive(message = "expectedDurationMonths must be > 0")
        private Double expectedDurationMonths;

        /** Switch target weights when leaving this regime (v1 expects weights to the other two regimes). */
        @NotNull(message = "switchWeights is required")
        @Valid
        private SwitchWeights switchWeights;

        @AssertTrue(message = "distributionType must be one of: normal, studentT")
        public boolean isSupportedDistributionType() {
            if (distributionType == null) return false;
            String t = distributionType.trim();
            return "normal".equals(t) || "studentT".equals(t);
        }

        @AssertTrue(message = "normal params required when distributionType=normal; studentT params required when distributionType=studentT")
        public boolean hasParamsForType() {
            if (distributionType == null) return false;
            String t = distributionType.trim();
            if ("normal".equals(t)) return normal != null;
            if ("studentT".equals(t)) return studentT != null;
            return false;
        }
    }

    @Getter
    @Setter
    public static class SwitchWeights {
        @NotNull(message = "toRegime0 is required")
        @PositiveOrZero(message = "toRegime0 must be >= 0")
        private Double toRegime0;

        @NotNull(message = "toRegime1 is required")
        @PositiveOrZero(message = "toRegime1 must be >= 0")
        private Double toRegime1;

        @NotNull(message = "toRegime2 is required")
        @PositiveOrZero(message = "toRegime2 must be >= 0")
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