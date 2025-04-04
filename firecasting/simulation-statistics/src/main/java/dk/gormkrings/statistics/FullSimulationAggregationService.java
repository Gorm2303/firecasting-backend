package dk.gormkrings.statistics;

import java.util.List;

public class FullSimulationAggregationService extends AbstractSimulationAggregationService {

    @Override
    protected List<Double> filterCapitals(List<Double> rawCapitals) {
        return rawCapitals; // Return the raw list without filtering.
    }
}
