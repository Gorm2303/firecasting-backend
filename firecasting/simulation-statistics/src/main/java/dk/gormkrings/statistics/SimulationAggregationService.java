package dk.gormkrings.statistics;

import dk.gormkrings.result.IResult;
import dk.gormkrings.result.ISnapshot;
import dk.gormkrings.simulation.data.Date;
import dk.gormkrings.simulation.result.Snapshot;
import java.util.*;
import java.util.stream.Collectors;
import static dk.gormkrings.statistics.StatisticsUtils.*;

public class SimulationAggregationService {

    private YearlySummaryCalculator summaryCalculator = new YearlySummaryCalculator();

    /**
     * Filters the raw DataPoint list by excluding outlier datapoints.
     * This method works on the datapoints grouped per year (e.g. year-end values).
     * In this robust version, we exclude datapoints that lie below the 5th percentile or above the 95th percentile.
     */
    private List<DataPoint> filterDataPoints(List<DataPoint> rawDataPoints) {
        List<Double> capitals = rawDataPoints.stream()
                .map(dp -> dp.capital)
                .collect(Collectors.toList());
        double lowerBound = quantile(capitals, 0.05);
        double upperBound = quantile(capitals, 0.95);
        return rawDataPoints.stream()
                .filter(dp -> dp.capital >= lowerBound && dp.capital <= upperBound)
                .collect(Collectors.toList());
    }

    /**
     * Aggregates simulation results into yearly summaries.
     * Before grouping snapshots by year, entire simulation runs are removed if their final effective capital
     * (i.e. the last snapshot's effective capital, with non‑deposit negatives set to 0) is among the bottom or top 5%.
     */
    public List<YearlySummary> aggregateResults(List<IResult> results) {
        // First, compute the final effective capital for each simulation run.
        Map<IResult, Double> runFinalCapital = new HashMap<>();
        for (IResult result : results) {
            boolean runFailed = false;
            double finalEffectiveCapital = 0.0;
            List<ISnapshot> snapshots = result.getSnapshots();
            for (ISnapshot snap : snapshots) {
                var state = ((Snapshot) snap).getState();
                double capital = state.getCapital();
                if (!"Deposit".equalsIgnoreCase(state.getPhaseName()) && capital <= 0) {
                    runFailed = true;
                }
                // Once a run has failed, effective capital becomes 0 for subsequent snapshots.
                finalEffectiveCapital = runFailed ? 0 : (capital < 0 ? 0 : capital);
            }
            runFinalCapital.put(result, finalEffectiveCapital);
        }

        // Compute thresholds (5th and 95th percentiles) over final effective capitals.
        List<Double> finals = new ArrayList<>(runFinalCapital.values());
        double lowerThreshold = quantile(finals, 0.05);
        double upperThreshold = quantile(finals, 0.95);

        // Filter out entire simulation runs that are outliers.
        List<IResult> filteredResults = runFinalCapital.entrySet().stream()
                .filter(e -> e.getValue() >= lowerThreshold && e.getValue() <= upperThreshold)
                .map(Map.Entry::getKey)
                .toList();

        // Group the snapshots from the filtered simulation runs by year.
        Map<Integer, List<DataPoint>> dataByYear = new HashMap<>();

        for (IResult result : filteredResults) {
            boolean runFailed = false;
            List<ISnapshot> snapshots = result.getSnapshots();
            for (ISnapshot snap : snapshots) {
                var state = ((Snapshot) snap).getState();
                int year = new Date((int) state.getStartTime())
                        .plusDays(state.getTotalDurationAlive())
                        .getYear();
                double capital = state.getCapital();
                if (!"Deposit".equalsIgnoreCase(state.getPhaseName()) && capital <= 0) {
                    runFailed = true;
                }
                double effectiveCapital = runFailed ? 0 : (capital < 0 ? 0 : capital);
                boolean negativeFlag = (!"Deposit".equalsIgnoreCase(state.getPhaseName())) && (capital <= 0);
                DataPoint dp = new DataPoint(effectiveCapital, negativeFlag);
                dataByYear.computeIfAbsent(year, y -> new ArrayList<>()).add(dp);
            }
        }

        // Build yearly summaries using the filtered datapoints.
        List<YearlySummary> summaries = new ArrayList<>();
        for (Integer year : dataByYear.keySet()) {
            List<DataPoint> rawDataPoints = dataByYear.get(year);
            // Apply robust filtering on a per-year basis.
            List<DataPoint> filteredData = filterDataPoints(rawDataPoints);
            List<Double> capitals = filteredData.stream().map(dp -> dp.capital).collect(Collectors.toList());
            List<Boolean> negatives = filteredData.stream().map(dp -> dp.negativeFlag).collect(Collectors.toList());
            YearlySummary summary = summaryCalculator.calculateYearlySummary(year, capitals, negatives);
            summaries.add(summary);
        }

        summaries.sort(Comparator.comparingInt(YearlySummary::getYear));

        // Compute year-to-year growth based on the filtered average capital.
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
     * DataPoint holds a capital value and a negative flag.
     */
    private static class DataPoint {
        public final double capital;
        public final boolean negativeFlag;
        public DataPoint(double capital, boolean negativeFlag) {
            this.capital = capital;
            this.negativeFlag = negativeFlag;
        }
    }
}
