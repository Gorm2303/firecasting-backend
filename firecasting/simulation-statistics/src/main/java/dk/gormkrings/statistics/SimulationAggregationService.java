package dk.gormkrings.statistics;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.gormkrings.dto.ProgressUpdate;
import dk.gormkrings.result.IRunResult;
import dk.gormkrings.result.ISnapshot;
import dk.gormkrings.simulation.IProgressCallback;
import dk.gormkrings.simulation.data.Date;
import dk.gormkrings.simulation.result.Snapshot;
import dk.gormkrings.statistics.MetricSummary;
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

    private double lowerThresholdPercentile = 0;
    private double upperThresholdPercentile = 1;

    // Composite key: (phase, year)
    private record Key(String phaseName, int year) {}
    private static final ObjectMapper OM = new ObjectMapper();


    /**
     * Aggregate into YearlySummary per (phase,year).
     * Order of the returned list: year ASC, then phaseName ASC.
     * Growth is computed per phase across years.
     */
    public List<YearlySummary> aggregateResults(List<IRunResult> results, String simulationId, IProgressCallback cb) {
        long t0 = System.currentTimeMillis();

        // Build filtered+marked data grouped by (phase,year) in deterministic key order
        LinkedHashMap<Key, List<DataPoint>> dataByKey = computeMarkedDataByPhaseYear(results);

        long t1 = System.currentTimeMillis();
        log.debug("Prepared data for summaries in {} ms", (t1 - t0));

        // Build summaries following the same key order
        List<YearlySummary> summaries = new ArrayList<>(dataByKey.size());
        buildYearlySummaries(dataByKey, summaries, cb);

        // Compute growth per phase across years (doesn't reorder the list)
        computeGrowthPerPhase(summaries);

        log.info("Built {} yearly summaries in {} ms", summaries.size(), (System.currentTimeMillis() - t1));
        return summaries;
    }

    /**
     * Build 101-point percentile grids (0.0%..100.0%) per (phase,year), NO interpolation.
     * Order matches aggregateResults: year ASC, phaseName ASC.
     */
    public List<Double[]> buildPercentileGrids(List<IRunResult> results) {
        var dataByKey = computeMarkedDataByPhaseYear(results);

        List<Double[]> grids = new ArrayList<>(dataByKey.size());
        for (var e : dataByKey.entrySet()) {
            Double[] samples = e.getValue().stream()
                    .filter(dp -> !dp.runFailed())
                    .mapToDouble(DataPoint::capital) // primitive
                    .sorted()
                    .boxed()                         // turn DoubleStream -> Stream<Double>
                    .toArray(Double[]::new);         // -> Double[]
            grids.add(buildNoInterpolationGrid(samples)); // 101 points
        }
        return grids;
    }

    /**
     * Compute percentile summaries for supported metrics:
     * - YEARLY: per (phase, year)
     * - PHASE_TOTAL: totals aggregated over the whole phase
     * - OVERALL_TOTAL: totals aggregated over all phases
     *
     * Metrics:
     * - capital (year/phase end)
     * - inflation (year/phase end)
     * - deposit/withdraw/tax/fee/return (summed deltas across the year/phase)
     */
    public List<MetricSummary> aggregateMetricSummaries(List<IRunResult> results) {
        if (results == null || results.isEmpty()) return List.of();

        record Key(String phaseName, int year) {
        }

        // Global aggregators across runs
        Map<Key, Map<String, List<Double>>> yearly = new HashMap<>();
        Map<String, Map<String, List<Double>>> phaseTotals = new HashMap<>();
        Map<String, List<Double>> overallTotals = new HashMap<>();

        for (IRunResult run : results) {
            if (run == null || run.getSnapshots() == null || run.getSnapshots().isEmpty()) continue;

            // Per-run accumulators
            Map<Key, Map<String, Double>> yearlySums = new HashMap<>();
            Map<Key, Double> yearlyLastCapital = new HashMap<>();
            Map<Key, Double> yearlyLastInflation = new HashMap<>();

            Map<String, Map<String, Double>> phaseSums = new HashMap<>();
            Map<String, Double> phaseLastInflation = new HashMap<>();

            Map<String, Double> overallSums = new HashMap<>();
            Double overallLastInflation = null;

            ISnapshot prevSnap = null;
            for (ISnapshot snap0 : run.getSnapshots()) {
                if (snap0 == null) continue;
                Snapshot snap = (Snapshot) snap0;
                var state = snap.getState();

                int year = new Date((int) state.getStartTime()).plusDays(state.getTotalDurationAlive()).getYear();
                String phase = state.getPhaseName();

                if (prevSnap == null) prevSnap = snap0;
                var prev = ((Snapshot) prevSnap).getState();

                double dDeposit = state.getDeposited() - prev.getDeposited();
                double dWithdraw = state.getWithdrawn() - prev.getWithdrawn();
                double dTax = state.getTax() - prev.getTax();
                double dFee = state.getFee() - prev.getFee();
                double dReturn = state.getReturned() - prev.getReturned();

                Key k = new Key(phase, year);
                Map<String, Double> sumsForKey = yearlySums.computeIfAbsent(k, __ -> new HashMap<>());
                sumsForKey.merge("deposit", dDeposit, Double::sum);
                sumsForKey.merge("withdraw", dWithdraw, Double::sum);
                sumsForKey.merge("tax", dTax, Double::sum);
                sumsForKey.merge("fee", dFee, Double::sum);
                sumsForKey.merge("return", dReturn, Double::sum);

                yearlyLastCapital.put(k, state.getCapital());
                yearlyLastInflation.put(k, state.getInflation());

                Map<String, Double> phaseSumsForPhase = phaseSums.computeIfAbsent(phase, __ -> new HashMap<>());
                phaseSumsForPhase.merge("deposit", dDeposit, Double::sum);
                phaseSumsForPhase.merge("withdraw", dWithdraw, Double::sum);
                phaseSumsForPhase.merge("tax", dTax, Double::sum);
                phaseSumsForPhase.merge("fee", dFee, Double::sum);
                phaseSumsForPhase.merge("return", dReturn, Double::sum);
                phaseLastInflation.put(phase, state.getInflation());

                overallSums.merge("deposit", dDeposit, Double::sum);
                overallSums.merge("withdraw", dWithdraw, Double::sum);
                overallSums.merge("tax", dTax, Double::sum);
                overallSums.merge("fee", dFee, Double::sum);
                overallSums.merge("return", dReturn, Double::sum);
                overallLastInflation = state.getInflation();

                prevSnap = snap0;
            }

            // Flush per-run yearly to global
            for (var e : yearlySums.entrySet()) {
                Key k = e.getKey();
                yearly.computeIfAbsent(k, __ -> new HashMap<>());

                // flows
                for (var me : e.getValue().entrySet()) {
                    yearly.get(k).computeIfAbsent(me.getKey(), __ -> new ArrayList<>()).add(me.getValue());
                }

                // end-of-year/phase series
                Double cap = yearlyLastCapital.get(k);
                if (cap != null) yearly.get(k).computeIfAbsent("capital", __ -> new ArrayList<>()).add(cap);
                Double infl = yearlyLastInflation.get(k);
                if (infl != null) yearly.get(k).computeIfAbsent("inflation", __ -> new ArrayList<>()).add(infl);
            }

            // Flush per-run phase totals
            for (var pe : phaseSums.entrySet()) {
                String phase = pe.getKey();
                phaseTotals.computeIfAbsent(phase, __ -> new HashMap<>());
                for (var me : pe.getValue().entrySet()) {
                    phaseTotals.get(phase).computeIfAbsent(me.getKey(), __ -> new ArrayList<>()).add(me.getValue());
                }
                Double infl = phaseLastInflation.get(phase);
                if (infl != null) phaseTotals.get(phase).computeIfAbsent("inflation", __ -> new ArrayList<>()).add(infl);
            }

            // Flush per-run overall totals
            for (var me : overallSums.entrySet()) {
                overallTotals.computeIfAbsent(me.getKey(), __ -> new ArrayList<>()).add(me.getValue());
            }
            if (overallLastInflation != null) {
                overallTotals.computeIfAbsent("inflation", __ -> new ArrayList<>()).add(overallLastInflation);
            }
        }

        List<MetricSummary> out = new ArrayList<>();

        // YEARLY
        yearly.entrySet().stream()
            .sorted(Comparator
                .comparing((Map.Entry<Key, ?> e) -> e.getKey().year())
                .thenComparing(e -> e.getKey().phaseName(), Comparator.nullsFirst(String::compareTo)))
            .forEach(e -> {
                Key k = e.getKey();
                e.getValue().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(me -> out.add(buildSummary(MetricSummary.Scope.YEARLY, k.phaseName(), k.year(), me.getKey(), me.getValue())));
            });

        // PHASE_TOTAL
        phaseTotals.entrySet().stream()
            .sorted(Map.Entry.comparingByKey(Comparator.nullsFirst(String::compareTo)))
            .forEach(e -> e.getValue().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(me -> out.add(buildSummary(MetricSummary.Scope.PHASE_TOTAL, e.getKey(), null, me.getKey(), me.getValue()))));

        // OVERALL_TOTAL
        overallTotals.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(me -> out.add(buildSummary(MetricSummary.Scope.OVERALL_TOTAL, null, null, me.getKey(), me.getValue())));

        return out;
    }

    private static MetricSummary buildSummary(MetricSummary.Scope scope, String phaseName, Integer year, String metric, List<Double> values) {
        List<Double> sorted = (values == null) ? List.of() : values.stream().filter(Objects::nonNull).sorted().toList();

        MetricSummary s = new MetricSummary();
        s.setScope(scope);
        s.setPhaseName(phaseName);
        s.setYear(year);
        s.setMetric(metric);

        if (sorted.isEmpty()) {
            s.setP5(Double.NaN);
            s.setP10(Double.NaN);
            s.setP25(Double.NaN);
            s.setP50(Double.NaN);
            s.setP75(Double.NaN);
            s.setP90(Double.NaN);
            s.setP95(Double.NaN);
            return s;
        }

        s.setP5(quantile(sorted, 0.05));
        s.setP10(quantile(sorted, 0.10));
        s.setP25(quantile(sorted, 0.25));
        s.setP50(quantile(sorted, 0.50));
        s.setP75(quantile(sorted, 0.75));
        s.setP90(quantile(sorted, 0.90));
        s.setP95(quantile(sorted, 0.95));
        return s;
    }

    // ---------- pipeline shared by summaries & grids ----------

    /**
     * Process → filter outliers by final capital → mark failures → group by (phase,year).
     * Returns LinkedHashMap in deterministic key order: year ASC, phaseName ASC.
     */
    private LinkedHashMap<Key, List<DataPoint>> computeMarkedDataByPhaseYear(List<IRunResult> results) {
        // 1) Extract per-run data
        List<SimulationRunData> runs = new ArrayList<>(results.size());
        for (IRunResult r : results) runs.add(processRun(r));
        if (runs.isEmpty()) return new LinkedHashMap<>();

        // 2) Outlier thresholds on final effective capitals
        List<Double> finals = runs.stream().map(SimulationRunData::finalEffectiveCapital).toList();
        List<Double> sortedFinals = new ArrayList<>(finals);
        Collections.sort(sortedFinals);
        double lower = quantile(sortedFinals, lowerThresholdPercentile);
        double upper = quantile(sortedFinals, upperThresholdPercentile);

        // 3) Filter inliers and mark failures forward
        List<SimulationRunData> marked = runs.stream()
                .filter(r -> r.finalEffectiveCapital() >= lower && r.finalEffectiveCapital() <= upper)
                .map(this::markFailures)
                .toList();

        // 4) Group by (phase,year)
        Map<Key, List<DataPoint>> tmp = new HashMap<>();
        for (SimulationRunData r : marked) {
            for (DataPoint dp : r.dataPoints()) {
                Key k = new Key(dp.phaseName(), dp.year());
                tmp.computeIfAbsent(k, __ -> new ArrayList<>()).add(dp);
            }
        }

        // 5) Deterministic iteration order: year ASC, then phaseName ASC
        return tmp.entrySet().stream()
                .sorted(Comparator
                        .comparing((Map.Entry<Key, ?> e) -> e.getKey().year())
                        .thenComparing(e -> e.getKey().phaseName(), Comparator.nullsFirst(String::compareTo)))
                .collect(LinkedHashMap::new,
                        (m, e) -> m.put(e.getKey(), e.getValue()),
                        LinkedHashMap::putAll);
    }

    // ---------- summaries & growth ----------

    private void buildYearlySummaries(LinkedHashMap<Key, List<DataPoint>> dataByKey,
                                      List<YearlySummary> out,
                                      IProgressCallback cb) {
        int total = dataByKey.size();
        long t0 = System.currentTimeMillis();
        int i = 0;

        for (Map.Entry<Key, List<DataPoint>> entry : dataByKey.entrySet()) {
            i++;
            Key k = entry.getKey();
            List<DataPoint> raw = entry.getValue();

            List<Double> capitals = raw.stream().map(DataPoint::capital).collect(Collectors.toList());
            List<Boolean> failed = raw.stream().map(DataPoint::runFailed).collect(Collectors.toList());

            YearlySummary summary = YearlySummaryCalculator.calculateYearlySummary(k.phaseName(), k.year(), capitals, failed);
            out.add(summary);

            long elapsedSec = (System.currentTimeMillis() - t0) / 1000;
            String human = String.format(
                    "Calculate %,d/%,d summaries (year=%d, phase=%s) in %,ds",
                    i, total, k.year(), k.phaseName(), elapsedSec
            );

            try {
                // send as typed JSON so the controller can pace/coalesce without parsing strings
                cb.update(OM.writeValueAsString(ProgressUpdate.message(human)));
                log.info(OM.writeValueAsString(ProgressUpdate.message(human)));
            } catch (Exception e) {
                // ultra-safe fallback: still send the human line
                cb.update(human);
            }
        }
    }

    /** Compute growth per phase across years, in-place. */
    private void computeGrowthPerPhase(List<YearlySummary> summaries) {
        // We assume summaries are ordered by year ASC, phaseName ASC.
        Map<String, Double> lastAvgByPhase = new HashMap<>();
        Map<String, Integer> lastYearByPhase = new HashMap<>();

        for (YearlySummary s : summaries) {
            String phase = s.getPhaseName();
            Double prevAvg = lastAvgByPhase.get(phase);
            if (prevAvg != null && prevAvg > 0) {
                double growthPct = ((s.getAverageCapital() / prevAvg) - 1.0) * 100.0;
                s.setCumulativeGrowthRate(growthPct);
            } else {
                s.setCumulativeGrowthRate(0.0);
            }
            lastAvgByPhase.put(phase, s.getAverageCapital());
            lastYearByPhase.put(phase, s.getYear());
        }
    }

    // ---------- per-run processing & failure marking ----------

    private SimulationRunData processRun(IRunResult result) {
        double finalEffectiveCapital = 0.0;
        List<DataPoint> dps = new ArrayList<>();
        for (ISnapshot snap : result.getSnapshots()) {
            var state = ((Snapshot) snap).getState();
            int year = new Date((int) state.getStartTime()).plusDays(state.getTotalDurationAlive()).getYear();
            double capital = state.getCapital();
            dps.add(new DataPoint(capital, false, year, state.getPhaseName()));
            finalEffectiveCapital = capital;
        }
        return new SimulationRunData(finalEffectiveCapital, dps);
    }

    private SimulationRunData markFailures(SimulationRunData runData) {
        boolean failed = false;
        List<DataPoint> out = new ArrayList<>(runData.dataPoints().size());
        for (DataPoint dp : runData.dataPoints()) {
            DataPoint failPoint = new DataPoint(0.0, true, dp.year(), dp.phaseName());
            if (!failed) {
                if (!"Deposit".equalsIgnoreCase(dp.phaseName()) && dp.capital() <= 0.0) {
                    failed = true;
                    out.add(failPoint);      // mark this and all next as failed
                } else {
                    out.add(dp);
                }
            } else {
                out.add(failPoint);
            }
        }
        double finalCap = out.isEmpty() ? 0.0 : out.get(out.size() - 1).capital();
        return new SimulationRunData(finalCap, out);
    }

    // ---------- grid builder: NO interpolation ----------

    /**
     * 101-point grid using EDF inverse (Type-1, no interpolation).
     * For p in {0/100..100/100}, index = ceil(p*n)-1 clamped to [0..n-1].
     * If no samples, returns an all-NaN grid.
     */
    private static Double[] buildNoInterpolationGrid(Double[] sortedAsc) {
        Double[] grid = new Double[101];
        int n = sortedAsc.length;
        if (n == 0) {
            Arrays.fill(grid, Double.NaN);
            return grid;
        }
        for (int i = 0; i <= 100; i++) {
            double p = i / 100.0;
            int idx;
            if (p <= 0.0) idx = 0;
            else if (p >= 1.0) idx = n - 1;
            else {
                idx = (int) Math.ceil(p * n) - 1;
                if (idx < 0) idx = 0;
                if (idx >= n) idx = n - 1;
            }
            grid[i] = sortedAsc[idx];
        }
        return grid;
    }

    // ---------- internal data carriers ----------

    private record SimulationRunData(double finalEffectiveCapital, List<DataPoint> dataPoints) {}
    private record DataPoint(double capital, boolean runFailed, int year, String phaseName) {}
}
