package dk.gormkrings.statistics;

import dk.gormkrings.result.IResult;
import dk.gormkrings.result.ISnapshot;
import dk.gormkrings.simulation.data.Date;
import dk.gormkrings.simulation.result.Snapshot;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static dk.gormkrings.statistics.StatisticsUtils.*;

@Component
@ConfigurationProperties(prefix = "statistics")
@Setter
@Getter
public class SimulationAggregationService {
    private final YearlySummaryCalculator summaryCalculator = new YearlySummaryCalculator();

    private double lowerThresholdPercentile = 0.05;
    private double upperThresholdPercentile = 0.95;

    /**
     * Aggregates simulation results into yearly summaries.
     * Entire simulation runs are first filtered out based on their final effective capital,
     * then snapshots from the remaining runs are grouped by year.
     */
    public List<YearlySummary> aggregateResults(List<IResult> results) {
        // Process each simulation run once to calculate final capital and collect its datapoints.
        List<SimulationRunData> simulationDataList = new ArrayList<>();
        for (IResult result : results) {
            simulationDataList.add(processRun(result));
        }

        // Compute thresholds over final effective capitals.
        List<Double> finals = simulationDataList.stream()
                .map(runData -> runData.finalEffectiveCapital)
                .collect(Collectors.toList());
        double lowerThreshold = quantile(finals, lowerThresholdPercentile);
        double upperThreshold = quantile(finals, upperThresholdPercentile);

        // Filter simulation runs based on computed thresholds.
        List<SimulationRunData> filteredSimulationData = simulationDataList.stream()
                .filter(runData -> runData.finalEffectiveCapital >= lowerThreshold &&
                        runData.finalEffectiveCapital <= upperThreshold)
                .toList();

        // Group all DataPoints from the filtered simulation runs by year.
        Map<Integer, List<DataPoint>> dataByYear = new HashMap<>();
        for (SimulationRunData runData : filteredSimulationData) {
            for (DataPoint dp : runData.dataPoints) {
                dataByYear.computeIfAbsent(dp.year, y -> new ArrayList<>()).add(dp);
            }
        }

        // Build yearly summaries using the grouped datapoints.
        List<YearlySummary> summaries = new ArrayList<>();
        for (Map.Entry<Integer, List<DataPoint>> entry : dataByYear.entrySet()) {
            int year = entry.getKey();
            String phaseName = entry.getValue().get(year).phaseName();
            List<DataPoint> rawDataPoints = entry.getValue();
            List<Double> capitals = rawDataPoints.stream()
                    .map(dp -> dp.capital)
                    .collect(Collectors.toList());
            List<Boolean> failed = rawDataPoints.stream()
                    .map(dp -> dp.runFailed)
                    .collect(Collectors.toList());
            YearlySummary summary = summaryCalculator.calculateYearlySummary(phaseName, year, capitals, failed);
            summaries.add(summary);
        }

        // Sort and compute year-to-year growth.
        summaries.sort(Comparator.comparingInt(YearlySummary::getYear));
        for (int i = 1; i < summaries.size(); i++) {
            YearlySummary previous = summaries.get(i - 1);
            YearlySummary current = summaries.get(i);
            if (previous.getAverageCapital() > 0) {
                double growth = ((current.getAverageCapital() / previous.getAverageCapital()) - 1) * 100;
                current.setCumulativeGrowthRate(growth);
            } else {
                current.setCumulativeGrowthRate(0.0);
            }
        }

        return summaries;
    }

    /**
     * Processes a simulation run (IResult) by iterating through its snapshots just once.
     * The method computes the final effective capital and collects a list of DataPoints,
     * where each DataPoint also stores its computed year.
     *
     * @param result the simulation run.
     * @return a SimulationRunData containing the final effective capital and associated datapoints.
     */
    private SimulationRunData processRun(IResult result) {
        boolean runFailed = false;
        double finalEffectiveCapital = 0.0;
        List<DataPoint> dataPoints = new ArrayList<>();
        for (ISnapshot snap : result.getSnapshots()) {
            var state = ((Snapshot) snap).getState();
            int year = new Date((int) state.getStartTime())
                    .plusDays(state.getTotalDurationAlive())
                    .getYear();
            double capital = state.getCapital();
            // Once a snapshot fails, the run is marked as failed.
            if (!"Deposit".equalsIgnoreCase(state.getPhaseName()) && capital <= 0) {
                runFailed = true;
            }
            // Use the same flag to determine the effective capital:
            double effectiveCapital = runFailed ? 0 : (capital < 0 ? 0 : capital);
            // Use the accumulated runFailed flag as the negative flag.
            dataPoints.add(new DataPoint(effectiveCapital, runFailed, year, state.getPhaseName()));
            // The final effective capital is taken from the last snapshot.
            finalEffectiveCapital = effectiveCapital;
        }
        return new SimulationRunData(finalEffectiveCapital, dataPoints);
    }


    /**
         * Holds the computed final effective capital and associated datapoints for a simulation run.
         */
        private record SimulationRunData(double finalEffectiveCapital, List<DataPoint> dataPoints) {
    }

    /**
         * DataPoint holds a capital value, a negative flag, and the year it belongs to.
         */
        private record DataPoint(double capital, boolean runFailed, int year, String phaseName) {
    }

}
