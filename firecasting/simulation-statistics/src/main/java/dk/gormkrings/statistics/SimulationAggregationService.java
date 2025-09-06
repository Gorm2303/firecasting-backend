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

    private double lowerThresholdPercentile = 0.05;
    private double upperThresholdPercentile = 0.95;

    // Composite key: (phase, year)
    private record Key(String phaseName, int year) {}

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
     * Build 1001-point percentile grids (0.0%..100.0%) per (phase,year), NO interpolation.
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
            grids.add(buildNoInterpolationGrid(samples)); // 1001 points
        }
        return grids;
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

            String msg = String.format("Calculate %,d/%,d summaries (year=%d, phase=%s) in %,ds",
                    i, total, k.year(), k.phaseName(), (System.currentTimeMillis() - t0) / 1000);
            cb.update(msg);
            log.info(msg);
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
     * 1001-point grid using EDF inverse (Type-1, no interpolation).
     * For p in {0/1000..1000/1000}, index = ceil(p*n)-1 clamped to [0..n-1].
     * If no samples, returns an all-NaN grid.
     */
    private static Double[] buildNoInterpolationGrid(Double[] sortedAsc) {
        Double[] grid = new Double[1001];
        int n = sortedAsc.length;
        if (n == 0) {
            Arrays.fill(grid, Double.NaN);
            return grid;
        }
        for (int i = 0; i <= 1000; i++) {
            double p = i / 1000.0;
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
