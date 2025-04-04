package dk.gormkrings.statistics;

import dk.gormkrings.result.IResult;
import dk.gormkrings.result.ISnapshot;
import dk.gormkrings.simulation.data.Date;
import dk.gormkrings.simulation.result.Snapshot;

import java.util.*;

public abstract class AbstractSimulationAggregationService {

    protected YearlySummaryCalculator summaryCalculator = new YearlySummaryCalculator();

    protected abstract List<Double> filterCapitals(List<Double> rawCapitals);

    public List<YearlySummary> aggregateResults(List<IResult> results) {
        // Group raw effective capital values by year and record negative events.
        Map<Integer, List<Double>> capitalByYear = new HashMap<>();
        Map<Integer, List<Boolean>> negativeOccurrenceByYear = new HashMap<>();

        // Process each simulation run.
        for (IResult result : results) {
            boolean runFailed = false;
            List<ISnapshot> snapshots = result.getSnapshots();
            for (ISnapshot snap : snapshots) {
                var state = ((Snapshot) snap).getState();
                int year = new Date((int) state.getStartTime())
                        .plusDays(state.getTotalDurationAlive())
                        .getYear();
                double capital = state.getCapital();
                // If snapshot is outside deposit phase and capital is <= 0, mark run as failed.
                if (!"Deposit".equalsIgnoreCase(state.getPhaseName()) && capital <= 0) {
                    runFailed = true;
                }
                // Use raw capital unless the run has failed; if failed, effective capital is 0.
                double effectiveCapital = runFailed ? 0 : (capital < 0 ? 0 : capital);
                capitalByYear.computeIfAbsent(year, y -> new ArrayList<>()).add(effectiveCapital);
                boolean negativeEvent = (!"Deposit".equalsIgnoreCase(state.getPhaseName())) && (capital <= 0);
                negativeOccurrenceByYear.computeIfAbsent(year, y -> new ArrayList<>()).add(negativeEvent);
            }
        }

        // Build yearly summaries.
        List<YearlySummary> summaries = new ArrayList<>();
        for (Integer year : capitalByYear.keySet()) {
            List<Double> rawCapitals = capitalByYear.get(year);
            // Delegate to the concrete subclass how to filter the capitals.
            List<Double> capitals = filterCapitals(rawCapitals);
            // Use the shared YearlySummaryCalculator to compute statistics.
            List<Boolean> negatives = negativeOccurrenceByYear.getOrDefault(year, Collections.emptyList());
            YearlySummary summary = summaryCalculator.calculateYearlySummary(year, capitals, negatives);
            summaries.add(summary);
        }

        // Sort summaries by year.
        summaries.sort(Comparator.comparingInt(YearlySummary::getYear));

        // Compute year-to-year growth based on the average capital (from the filtered data).
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
}
