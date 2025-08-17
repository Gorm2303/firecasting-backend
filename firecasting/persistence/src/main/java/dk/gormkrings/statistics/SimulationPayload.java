package dk.gormkrings.statistics;

import java.util.List;

// Simple request shape: your input DTO + your YearlySummary DTOs + 1001-point grids
public record SimulationPayload(
        Object inputParams,
        List<YearlySummary> summaries,
        List<double[]> percentileGrids
) {}
