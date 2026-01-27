package dk.gormkrings.statistics;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * Percentile summaries for numeric metrics.
 *
 * This is used for:
 * - YEARLY: a yearly metric series per (phase, year)
 * - PHASE_TOTAL: totals aggregated over the whole phase
 * - OVERALL_TOTAL: totals aggregated over all phases
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetricSummary {

    public enum Scope {
        YEARLY,
        PHASE_TOTAL,
        OVERALL_TOTAL
    }

    private Scope scope;

    /** Present for YEARLY and PHASE_TOTAL. */
    private String phaseName;

    /** Present for YEARLY. */
    private Integer year;

    /** Metric identifier (e.g. capital, deposit, withdraw, tax, fee, return, inflation). */
    private String metric;

    private double p5;
    private double p10;
    private double p25;
    private double p50;
    private double p75;
    private double p90;
    private double p95;
}
