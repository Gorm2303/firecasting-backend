package dk.gormkrings.statistics;

import java.util.List;
import java.util.stream.Collectors;
import static dk.gormkrings.statistics.StatisticsUtils.*;

public class RobustSimulationAggregationService extends AbstractSimulationAggregationService {

    @Override
    protected List<Double> filterCapitals(List<Double> rawCapitals) {
        // Compute the 5th and 95th percentiles.
        double lowerBound = quantile(rawCapitals, 0.05);
        double upperBound = quantile(rawCapitals, 0.95);
        // Return only values within these bounds.
        return rawCapitals.stream()
                .filter(v -> v >= lowerBound && v <= upperBound)
                .collect(Collectors.toList());
    }
}
