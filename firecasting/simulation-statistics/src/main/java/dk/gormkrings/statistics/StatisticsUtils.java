package dk.gormkrings.statistics;

import java.util.*;
public class StatisticsUtils {

    public static double average(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    public static double median(List<Double> values) {
        if (values.isEmpty()) return 0.0;
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int n = sorted.size();
        return (n % 2 == 0)
                ? (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0
                : sorted.get(n / 2);
    }

    public static double stdDev(List<Double> values, double mean) {
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average().orElse(0.0);
        return Math.sqrt(variance);
    }

    public static double quantile(List<Double> values, double q) {
        if (values.isEmpty()) return 0.0;
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int n = sorted.size();
        double pos = (n - 1) * q;
        int index = (int) Math.floor(pos);
        double frac = pos - index;
        if (index + 1 < n) {
            return sorted.get(index) * (1 - frac) + sorted.get(index + 1) * frac;
        } else {
            return sorted.get(index);
        }
    }

    public static double trimmedMean(List<Double> values, double trimFraction) {
        if (values.isEmpty()) return 0.0;
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int trimCount = (int) (sorted.size() * trimFraction);
        if (sorted.size() - 2 * trimCount <= 0) {
            return average(sorted);
        }
        List<Double> trimmed = sorted.subList(trimCount, sorted.size() - trimCount);
        return average(trimmed);
    }
}
