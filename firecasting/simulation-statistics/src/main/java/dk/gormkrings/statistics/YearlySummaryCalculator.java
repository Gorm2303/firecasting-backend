package dk.gormkrings.statistics;

import java.util.*;
import java.util.stream.Collectors;
import static dk.gormkrings.statistics.StatisticsUtils.*;

public class YearlySummaryCalculator {

    /**
     * Calculates a YearlySummary based on a list of effective capital values for a given year.
     * Outliers are excluded by filtering out values below the 5th and above the 95th percentiles.
     */
    public YearlySummary calculateYearlySummary(int year, List<Double> capitals, List<Boolean> negativeFlags) {
        // Calculate the lower and upper bounds (5th and 95th percentiles).
        double lowerBound = quantile(capitals, 0.05);
        double upperBound = quantile(capitals, 0.95);
        // Exclude outliers.
        List<Double> filtered = capitals.stream()
                .filter(v -> v >= lowerBound && v <= upperBound)
                .collect(Collectors.toList());

        double robustAvg = average(filtered);
        double med = median(filtered);
        double min = filtered.isEmpty() ? 0.0 : Collections.min(filtered);
        double max = filtered.isEmpty() ? 0.0 : Collections.max(filtered);
        double sd = stdDev(filtered, robustAvg);
        double q5 = quantile(filtered, 0.05);
        double q25 = quantile(filtered, 0.25);
        double q75 = quantile(filtered, 0.75);
        double q95 = quantile(filtered, 0.95);

        // VaR is the 5th percentile; CVaR is the average of values below that threshold.
        double var = q5;
        List<Double> belowVaR = filtered.stream().filter(v -> v <= var).collect(Collectors.toList());
        double cvar = belowVaR.isEmpty() ? 0.0 : average(belowVaR);

        double negativePercentage = negativeFlags.isEmpty() ? 0.0 :
                negativeFlags.stream().filter(b -> b).count() * 100.0 / negativeFlags.size();

        YearlySummary summary = new YearlySummary();
        summary.setYear(year);
        summary.setAverageCapital(robustAvg);
        summary.setMedianCapital(med);
        summary.setMinCapital(min);
        summary.setMaxCapital(max);
        summary.setStdDevCapital(sd);
        summary.setQuantile5(q5);
        summary.setQuantile25(q25);
        summary.setQuantile75(q75);
        summary.setQuantile95(q95);
        summary.setVar(var);
        summary.setCvar(cvar);
        summary.setNegativeCapitalPercentage(negativePercentage);
        // Growth will be set later.
        summary.setCumulativeGrowthRate(0.0);
        return summary;
    }
}
