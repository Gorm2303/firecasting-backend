package dk.gormkrings.diff;

import dk.gormkrings.statistics.persistence.YearlySummaryEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RunDiffComparator {

    private RunDiffComparator() {
    }

    public record ComparisonResult(boolean exactMatch, boolean withinTolerance, int mismatches, double maxAbsDiff) {
    }

    public static ComparisonResult compare(
            List<YearlySummaryEntity> a,
            List<YearlySummaryEntity> b,
            double eps) {

        Map<String, YearlySummaryEntity> byKey = new HashMap<>();
        if (a != null) {
            for (YearlySummaryEntity e : a) {
                if (e == null) continue;
                byKey.put(key(e.getPhaseName(), e.getYear()), e);
            }
        }

        boolean exact = true;
        boolean tolOk = true;
        int mismatches = 0;
        double maxAbs = 0.0;

        if (b == null || b.isEmpty()) {
            return new ComparisonResult(false, false, 1, 0.0);
        }

        for (YearlySummaryEntity right : b) {
            if (right == null) continue;

            String k = key(right.getPhaseName(), right.getYear());
            YearlySummaryEntity left = byKey.remove(k);
            if (left == null) {
                exact = false;
                tolOk = false;
                mismatches++;
                continue;
            }

            boolean entryMismatch = false;

            // Scalars (update exact/tolerance and maxAbs)
            {
                double d;

                d = Math.abs(left.getAverageCapital() - right.getAverageCapital());
                if (Double.compare(left.getAverageCapital(), right.getAverageCapital()) != 0) exact = false;
                if (d > eps) { tolOk = false; entryMismatch = true; }
                maxAbs = Math.max(maxAbs, d);

                d = Math.abs(left.getMedianCapital() - right.getMedianCapital());
                if (Double.compare(left.getMedianCapital(), right.getMedianCapital()) != 0) exact = false;
                if (d > eps) { tolOk = false; entryMismatch = true; }
                maxAbs = Math.max(maxAbs, d);

                d = Math.abs(left.getMinCapital() - right.getMinCapital());
                if (Double.compare(left.getMinCapital(), right.getMinCapital()) != 0) exact = false;
                if (d > eps) { tolOk = false; entryMismatch = true; }
                maxAbs = Math.max(maxAbs, d);

                d = Math.abs(left.getMaxCapital() - right.getMaxCapital());
                if (Double.compare(left.getMaxCapital(), right.getMaxCapital()) != 0) exact = false;
                if (d > eps) { tolOk = false; entryMismatch = true; }
                maxAbs = Math.max(maxAbs, d);

                d = Math.abs(left.getStdDevCapital() - right.getStdDevCapital());
                if (Double.compare(left.getStdDevCapital(), right.getStdDevCapital()) != 0) exact = false;
                if (d > eps) { tolOk = false; entryMismatch = true; }
                maxAbs = Math.max(maxAbs, d);

                d = Math.abs(left.getCumulativeGrowthRate() - right.getCumulativeGrowthRate());
                if (Double.compare(left.getCumulativeGrowthRate(), right.getCumulativeGrowthRate()) != 0) exact = false;
                if (d > eps) { tolOk = false; entryMismatch = true; }
                maxAbs = Math.max(maxAbs, d);

                d = Math.abs(left.getQuantile5() - right.getQuantile5());
                if (Double.compare(left.getQuantile5(), right.getQuantile5()) != 0) exact = false;
                if (d > eps) { tolOk = false; entryMismatch = true; }
                maxAbs = Math.max(maxAbs, d);

                d = Math.abs(left.getQuantile25() - right.getQuantile25());
                if (Double.compare(left.getQuantile25(), right.getQuantile25()) != 0) exact = false;
                if (d > eps) { tolOk = false; entryMismatch = true; }
                maxAbs = Math.max(maxAbs, d);

                d = Math.abs(left.getQuantile75() - right.getQuantile75());
                if (Double.compare(left.getQuantile75(), right.getQuantile75()) != 0) exact = false;
                if (d > eps) { tolOk = false; entryMismatch = true; }
                maxAbs = Math.max(maxAbs, d);

                d = Math.abs(left.getQuantile95() - right.getQuantile95());
                if (Double.compare(left.getQuantile95(), right.getQuantile95()) != 0) exact = false;
                if (d > eps) { tolOk = false; entryMismatch = true; }
                maxAbs = Math.max(maxAbs, d);

                d = Math.abs(left.getVar() - right.getVar());
                if (Double.compare(left.getVar(), right.getVar()) != 0) exact = false;
                if (d > eps) { tolOk = false; entryMismatch = true; }
                maxAbs = Math.max(maxAbs, d);

                d = Math.abs(left.getCvar() - right.getCvar());
                if (Double.compare(left.getCvar(), right.getCvar()) != 0) exact = false;
                if (d > eps) { tolOk = false; entryMismatch = true; }
                maxAbs = Math.max(maxAbs, d);

                d = Math.abs(left.getNegativeCapitalPercentage() - right.getNegativeCapitalPercentage());
                if (Double.compare(left.getNegativeCapitalPercentage(), right.getNegativeCapitalPercentage()) != 0) exact = false;
                if (d > eps) { tolOk = false; entryMismatch = true; }
                maxAbs = Math.max(maxAbs, d);
            }

            if (entryMismatch) {
                mismatches++;
            }
        }

        // Any remaining entries were present in A but missing in B
        if (!byKey.isEmpty()) {
            exact = false;
            tolOk = false;
            mismatches += byKey.size();
        }

        return new ComparisonResult(exact, tolOk, mismatches, maxAbs);
    }

    private static String key(String phaseName, int year) {
        return phaseName + "#" + year;
    }

    // no helpers; compare logic kept explicit for clarity
}
