package dk.gormkrings.statistics;

import java.util.*;
import java.util.stream.Collectors;

public interface YearlySummaryCalculator {

    /**
     * Calculates a YearlySummary based on a list of effective capital values for a given year.
     * Outliers are excluded by filtering out values below the 5th and above the 95th percentiles.
     */
    static YearlySummary calculateYearlySummary(String phaseName, int year, List<Double> capitals, List<Boolean> negativeFlags) {
        List<Double> sorted = new ArrayList<>(capitals);
        Collections.sort(sorted);
        double robustAvg = StatisticsUtils.average(capitals);
        double med = StatisticsUtils.median(sorted);
        double min = capitals.isEmpty() ? 0.0 : Collections.min(capitals);
        double max = capitals.isEmpty() ? 0.0 : Collections.max(capitals);
        double sd = StatisticsUtils.stdDev(capitals, robustAvg);

        double q5 = StatisticsUtils.quantile(sorted, 0.05);
        double q25 = StatisticsUtils.quantile(sorted, 0.25);
        double q75 = StatisticsUtils.quantile(sorted, 0.75);
        double q95 = StatisticsUtils.quantile(sorted, 0.95);

        // VaR is the 5th percentile; CVaR is the average of values below that threshold.
        List<Double> belowVaR = capitals.stream().filter(v -> v <= q5).collect(Collectors.toList());
        double cvar = belowVaR.isEmpty() ? 0.0 : StatisticsUtils.average(belowVaR);

        double negativePercentage = negativeFlags.isEmpty() ? 0.0 :
                negativeFlags.stream().filter(b -> b).count() * 100.0 / negativeFlags.size();

        YearlySummary summary = new YearlySummary();
        summary.setPhaseName(phaseName);
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
        summary.setVar(q5);
        summary.setCvar(cvar);
        summary.setNegativeCapitalPercentage(negativePercentage);
        // Growth will be set later.
        summary.setCumulativeGrowthRate(0.0);
        return summary;
    }
}
