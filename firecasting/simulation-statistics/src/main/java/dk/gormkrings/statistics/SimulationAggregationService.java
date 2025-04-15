package dk.gormkrings.statistics;

import dk.gormkrings.result.IResult;
import dk.gormkrings.result.ISnapshot;
import dk.gormkrings.simulation.data.Date;
import dk.gormkrings.simulation.result.Snapshot;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static dk.gormkrings.statistics.StatisticsUtils.*;

@Slf4j
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
     * Steps:
     *   1) Process each simulation run to compute final capital and collect snapshots (without marking snapshot failures).
     *   2) Compute quantile thresholds over final effective capitals and filter out simulation runs that are outliers.
     *   3) For the remaining runs, mark snapshots as failed only from the snapshot that first fails (and onward).
     *   4) Group snapshots by year and build yearly summaries.
     */
    public List<YearlySummary> aggregateResults(List<IResult> results) {
        // Step 1: Process each simulation run (no failure marking).
        List<SimulationRunData> simulationDataList = new ArrayList<>();
        for (IResult result : results) {
            simulationDataList.add(processRun(result));
        }

        // Step 2: Compute quantile thresholds over final effective capitals.
        List<Double> finals = simulationDataList.stream()
                .map(runData -> runData.finalEffectiveCapital())
                .collect(Collectors.toList());
        double lowerThreshold = quantile(finals, lowerThresholdPercentile);
        double upperThreshold = quantile(finals, upperThresholdPercentile);

        // Filter out simulation runs that fall outside the desired quantile thresholds.
        List<SimulationRunData> filteredSimulationData = simulationDataList.stream()
                .filter(runData -> runData.finalEffectiveCapital() >= lowerThreshold &&
                        runData.finalEffectiveCapital() <= upperThreshold)
                .toList();

        // Step 3: For each filtered run, mark snapshots as failed only from the first failing snapshot onward.
        List<SimulationRunData> markedSimulationData = filteredSimulationData.stream()
                .map(this::markFailures)
                .collect(Collectors.toList());

        // Step 4: Group all DataPoints (snapshots) from the marked simulation runs by year.
        Map<Integer, List<DataPoint>> dataByYear = new HashMap<>();
        for (SimulationRunData runData : markedSimulationData) {
            for (DataPoint dp : runData.dataPoints()) {
                dataByYear.computeIfAbsent(dp.year(), y -> new ArrayList<>()).add(dp);
            }
        }

        // Build yearly summaries using the grouped datapoints.
        List<YearlySummary> summaries = new ArrayList<>();
        for (Map.Entry<Integer, List<DataPoint>> entry : dataByYear.entrySet()) {
            int year = entry.getKey();
            // Assume that within the same year, snapshots share the same phaseName.
            String phaseName = entry.getValue().get(0).phaseName();
            List<DataPoint> rawDataPoints = entry.getValue();
            List<Double> capitals = rawDataPoints.stream()
                    .map(dp -> dp.capital())
                    .collect(Collectors.toList());
            List<Boolean> failed = rawDataPoints.stream()
                    .map(dp -> dp.runFailed())
                    .collect(Collectors.toList());
            YearlySummary summary = summaryCalculator.calculateYearlySummary(phaseName, year, capitals, failed);
            summaries.add(summary);
        }

        // Sort the yearly summaries and compute the year-to-year growth.
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
     * Processes a simulation run (IResult) by iterating through its snapshots once.
     * This method collects snapshots into DataPoints without marking any failure.
     * The final effective capital is taken from the last snapshot.
     */
    private SimulationRunData processRun(IResult result) {
        double finalEffectiveCapital = 0.0;
        List<DataPoint> dataPoints = new ArrayList<>();
        for (ISnapshot snap : result.getSnapshots()) {
            var state = ((Snapshot) snap).getState();
            int year = new Date((int) state.getStartTime())
                    .plusDays(state.getTotalDurationAlive())
                    .getYear();
            double capital = state.getCapital();
            // Initially, do not mark run failure; record snapshot as is.
            dataPoints.add(new DataPoint(capital, false, year, state.getPhaseName()));
            finalEffectiveCapital = capital;
        }
        return new SimulationRunData(finalEffectiveCapital, dataPoints);
    }

    /**
     * For a given simulation run, mark snapshots (on a snapshot level) as failed only from the first
     * snapshot that fails (outside the "Deposit" phase with capital <= 0) and for all following snapshots.
     * Snapshots before the failure event remain unchanged.
     */
    private SimulationRunData markFailures(SimulationRunData runData) {
        boolean failureOccurred = false;
        List<DataPoint> updatedDataPoints = new ArrayList<>();
        for (DataPoint dp : runData.dataPoints()) {
            if (!failureOccurred) {
                // Check if this snapshot should trigger failure.
                if (!"Deposit".equalsIgnoreCase(dp.phaseName()) && dp.capital() <= 0) {
                    failureOccurred = true;
                    // Mark this snapshot as failed.
                    updatedDataPoints.add(new DataPoint(0, true, dp.year(), dp.phaseName()));
                } else {
                    updatedDataPoints.add(dp);
                }
            } else {
                // Once failure has occurred, mark all subsequent snapshots as failed.
                updatedDataPoints.add(new DataPoint(0, true, dp.year(), dp.phaseName()));
            }
        }
        double updatedFinalCapital = updatedDataPoints.isEmpty() ? 0 :
                updatedDataPoints.get(updatedDataPoints.size()-1).capital();
        return new SimulationRunData(updatedFinalCapital, updatedDataPoints);
    }

    /**
     * Holds the computed final effective capital and associated datapoints for a simulation run.
     */
    private record SimulationRunData(double finalEffectiveCapital, List<DataPoint> dataPoints) {}

    /**
     * DataPoint holds a capital value, a flag indicating if that snapshot (or its run, from that point onward)
     * is marked as failed, the associated year, and the phase name.
     */
    private record DataPoint(double capital, boolean runFailed, int year, String phaseName) {}
}
