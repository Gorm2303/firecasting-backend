package dk.gormkrings.statistics;

import dk.gormkrings.result.IRunResult;
import dk.gormkrings.result.ISnapshot;
import dk.gormkrings.simulation.IProgressCallback;
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
    public List<YearlySummary> aggregateResults(List<IRunResult> results, IProgressCallback callback) {
        long startTime = System.currentTimeMillis();
        // Step 1: Process each simulation run.
        List<SimulationRunData> simulationDataList = new ArrayList<>();
        for (IRunResult result : results) {
            simulationDataList.add(processRun(result));
            if (result == results.getFirst()) {
                log.debug("Aggregating and Processing run result: {}", result.getSnapshots());
            }
        }
        long processTime = System.currentTimeMillis();
        log.debug("Aggregating and Process each simulation run in {} ms", processTime - startTime);

        // Step 2: Compute quantile thresholds over final effective capitals.
        List<Double> finals = simulationDataList.stream()
                .map(SimulationRunData::finalEffectiveCapital)
                .toList();
        if (finals.isEmpty()) {
            log.warn("All simulation runs produced NaN or infinite final capital—no data to aggregate");
            return Collections.emptyList();
        }
        int counter = 1;
        for (SimulationRunData runData : simulationDataList) {
            double cap = runData.finalEffectiveCapital();
            if (Double.isNaN(cap) || Double.isInfinite(cap)) {
                log.error("Run resulted in invalid capital: {}, counter: {}", cap, counter);
                // optionally throw or skip right here
                counter++;
            }
        }

        List<Double> sorted = new ArrayList<>(finals);
        Collections.sort(sorted);
        log.debug("Aggregating and Sorted list of simulation run data: {}", sorted);
        double lowerThreshold = quantile(sorted, lowerThresholdPercentile);
        double upperThreshold = quantile(sorted, upperThresholdPercentile);
        log.debug("Aggregating and Lower threshold percentile = {}", lowerThreshold);
        log.debug("Aggregating and Upper threshold percentile = {}", upperThreshold);

        // Filter out simulation runs that fall outside the desired quantile thresholds.
        List<SimulationRunData> filteredSimulationData = simulationDataList.stream()
                .filter(runData -> runData.finalEffectiveCapital() >= lowerThreshold &&
                        runData.finalEffectiveCapital() <= upperThreshold)
                .toList();
        log.debug("Filtered list of simulation run data: {}", filteredSimulationData);
        // Step 3: For each filtered run, mark snapshots as failed only from the first failing snapshot onward.
        List<SimulationRunData> markedSimulationData = filteredSimulationData.stream()
                .map(this::markFailures)
                .toList();

        // Step 4: Group all DataPoints (snapshots) from the marked simulation runs by year.
        Map<Integer, List<DataPoint>> dataByYear = new HashMap<>();
        for (SimulationRunData runData : markedSimulationData) {
            for (DataPoint dp : runData.dataPoints()) {
                dataByYear.computeIfAbsent(dp.year(), y -> new ArrayList<>()).add(dp);
            }
        }

        startTime = System.currentTimeMillis();
        List<YearlySummary> summaries = new ArrayList<>();

        buildYearlySummaries(dataByYear, summaries, callback);

        long summaryBuildTime = System.currentTimeMillis();
        log.info("Built yearly summaries in {} ms", summaryBuildTime - startTime);

        sortComputeGrowth(summaries);

        return summaries;
    }

    private void sortComputeGrowth(List<YearlySummary> summaries) {
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
    }

    private void buildYearlySummaries(Map<Integer, List<DataPoint>> dataByYear, List<YearlySummary> summaries, IProgressCallback callback) {
        int counter = 0;
        long startTime = System.currentTimeMillis();
        for (Map.Entry<Integer, List<DataPoint>> entry : dataByYear.entrySet()) {
            counter++;
            int year = entry.getKey();
            String phaseName = entry.getValue().getFirst().phaseName();
            List<DataPoint> rawDataPoints = entry.getValue();
            List<Double> capitals = rawDataPoints.stream()
                    .map(DataPoint::capital)
                    .collect(Collectors.toList());
            List<Boolean> failed = rawDataPoints.stream()
                    .map(DataPoint::runFailed)
                    .collect(Collectors.toList());

            long yearStartTime = System.currentTimeMillis();
            YearlySummary summary = summaryCalculator.calculateYearlySummary(phaseName, year, capitals, failed);
            String progressMessage = String.format("Calculate %,d/%,d yearly summaries in %,ds",
                    counter, dataByYear.size(),
                    (yearStartTime - startTime)/1000);
            log.info(progressMessage);
            callback.update(progressMessage);
            summaries.add(summary);
        }
    }

    private SimulationRunData processRun(IRunResult result) {
        double finalEffectiveCapital = 0.0;
        List<DataPoint> dataPoints = new ArrayList<>();
        for (ISnapshot snap : result.getSnapshots()) {
            var state = ((Snapshot) snap).getState();
            int year = new Date((int) state.getStartTime())
                    .plusDays(state.getTotalDurationAlive())
                    .getYear();
            double capital = state.getCapital();
            dataPoints.add(new DataPoint(capital, false, year, state.getPhaseName()));
            finalEffectiveCapital = capital;
        }
        return new SimulationRunData(finalEffectiveCapital, dataPoints);
    }

    private SimulationRunData markFailures(SimulationRunData runData) {
        boolean failureOccurred = false;
        List<DataPoint> updatedDataPoints = new ArrayList<>();
        for (DataPoint dp : runData.dataPoints()) {
            DataPoint e = new DataPoint(0, true, dp.year(), dp.phaseName());
            if (!failureOccurred) {
                // Check if this snapshot should trigger failure.
                if (!"Deposit".equalsIgnoreCase(dp.phaseName()) && dp.capital() <= 0) {
                    failureOccurred = true;
                    // Mark this snapshot as failed.
                    updatedDataPoints.add(e);
                } else {
                    updatedDataPoints.add(dp);
                }
            } else {
                // Once failure has occurred, mark all subsequent snapshots as failed.
                updatedDataPoints.add(e);
            }
        }
        double updatedFinalCapital = updatedDataPoints.isEmpty() ? 0 :
                updatedDataPoints.getLast().capital();
        return new SimulationRunData(updatedFinalCapital, updatedDataPoints);
    }

    private record SimulationRunData(double finalEffectiveCapital, List<DataPoint> dataPoints) {}

    private record DataPoint(double capital, boolean runFailed, int year, String phaseName) {}
}
