package dk.gormkrings.reproducibility;

import dk.gormkrings.export.ReproducibilityBundleDto;
import dk.gormkrings.statistics.persistence.YearlySummaryEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ReproducibilityReplayComparator {

    private ReproducibilityReplayComparator() {
    }

    public record ComparisonResult(boolean exactMatch, boolean withinTolerance, int mismatches, double maxAbsDiff) {
    }

    public static ComparisonResult compare(
            List<YearlySummaryEntity> actual,
            List<ReproducibilityBundleDto.YearlySummaryWithGrid> expected,
            double eps) {

        if (expected == null) {
            return new ComparisonResult(false, false, 0, 0.0);
        }

        Map<String, YearlySummaryEntity> byKey = new HashMap<>();
        if (actual != null) {
            for (YearlySummaryEntity e : actual) {
                if (e == null) continue;
                byKey.put(key(e.getPhaseName(), e.getYear()), e);
            }
        }

        boolean exact = true;
        boolean tolOk = true;
        int mismatches = 0;
        double maxAbs = 0.0;

        for (var exp : expected) {
            if (exp == null) continue;
            YearlySummaryEntity act = byKey.get(key(exp.getPhaseName(), exp.getYear()));
            if (act == null) {
                exact = false;
                tolOk = false;
                mismatches++;
                continue;
            }

            // Compare scalar doubles
            maxAbs = max(maxAbs, cmp("averageCapital", act.getAverageCapital(), exp.getAverageCapital(), eps));
            maxAbs = max(maxAbs, cmp("medianCapital", act.getMedianCapital(), exp.getMedianCapital(), eps));
            maxAbs = max(maxAbs, cmp("minCapital", act.getMinCapital(), exp.getMinCapital(), eps));
            maxAbs = max(maxAbs, cmp("maxCapital", act.getMaxCapital(), exp.getMaxCapital(), eps));
            maxAbs = max(maxAbs, cmp("stdDevCapital", act.getStdDevCapital(), exp.getStdDevCapital(), eps));
            maxAbs = max(maxAbs, cmp("cumulativeGrowthRate", act.getCumulativeGrowthRate(), exp.getCumulativeGrowthRate(), eps));
            maxAbs = max(maxAbs, cmp("quantile5", act.getQuantile5(), exp.getQuantile5(), eps));
            maxAbs = max(maxAbs, cmp("quantile25", act.getQuantile25(), exp.getQuantile25(), eps));
            maxAbs = max(maxAbs, cmp("quantile75", act.getQuantile75(), exp.getQuantile75(), eps));
            maxAbs = max(maxAbs, cmp("quantile95", act.getQuantile95(), exp.getQuantile95(), eps));
            maxAbs = max(maxAbs, cmp("var", act.getVar(), exp.getVar(), eps));
            maxAbs = max(maxAbs, cmp("cvar", act.getCvar(), exp.getCvar(), eps));
            maxAbs = max(maxAbs, cmp("negativeCapitalPercentage", act.getNegativeCapitalPercentage(), exp.getNegativeCapitalPercentage(), eps));

            // Compare percentiles grid
            Double[] expGrid = exp.getPercentiles();
            Double[] actGrid = act.getPercentiles();

            if (!Objects.equals(expGrid == null ? 0 : expGrid.length, actGrid == null ? 0 : actGrid.length)) {
                exact = false;
                tolOk = false;
                mismatches++;
                continue;
            }
            if (expGrid != null) {
                for (int i = 0; i < expGrid.length; i++) {
                    Double ev = expGrid[i];
                    Double av = actGrid[i];
                    if (ev == null || av == null) {
                        if (ev != av) {
                            exact = false;
                            tolOk = false;
                            mismatches++;
                            break;
                        }
                        continue;
                    }
                    if (Double.compare(av, ev) != 0) {
                        exact = false;
                    }
                    double d = Math.abs(av - ev);
                    if (d > eps) {
                        tolOk = false;
                    }
                    if (d > 0) {
                        maxAbs = Math.max(maxAbs, d);
                    }
                }
            }
        }

        if (!exact || !tolOk) {
            // crude mismatch count: if we have any non-zero diff, count as mismatch
            // (kept intentionally simple; report includes maxAbsDiff for sizing)
            mismatches = Math.max(mismatches, exact ? 0 : 1);
        }

        return new ComparisonResult(exact, tolOk, mismatches, maxAbs);
    }

    private static String key(String phaseName, int year) {
        return phaseName + "#" + year;
    }

    private static double max(double a, double b) {
        return Math.max(a, b);
    }

    private static double cmp(String _name, double actual, double expected, double eps) {
        if (Double.compare(actual, expected) == 0) {
            return 0.0;
        }
        double d = Math.abs(actual - expected);
        return d;
    }
}
