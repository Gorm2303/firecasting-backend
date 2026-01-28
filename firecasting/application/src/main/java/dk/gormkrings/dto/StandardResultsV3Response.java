package dk.gormkrings.dto;

import dk.gormkrings.statistics.MetricSummary;
import dk.gormkrings.statistics.YearlySummary;
import lombok.Data;

import java.util.List;

/**
 * Standard simulation results payload (v3).
 *
 * This keeps the existing yearly summaries for capital distribution, and adds
 * percentile summaries for core flow metrics and totals.
 */
@Data
public class StandardResultsV3Response {
    private String simulationId;

    /** Existing yearly summaries (capital distribution etc). */
    private List<YearlySummary> yearlySummaries;

    /** Percentile summaries for supported metrics (yearly series + totals). */
    private List<MetricSummary> metricSummaries;
}
