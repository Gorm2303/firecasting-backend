package dk.gormkrings.simulation;

import dk.gormkrings.action.IAction;
import dk.gormkrings.action.IActionFactory;
import dk.gormkrings.dto.PhaseRequest;
import dk.gormkrings.dto.SimulationRequest;
import dk.gormkrings.factory.IDateFactory;
import dk.gormkrings.factory.IPhaseFactory;
import dk.gormkrings.factory.ISimulationFactory;
import dk.gormkrings.factory.ISpecificationFactory;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.result.IRunResult;
import dk.gormkrings.statistics.SimulationAggregationService;
import dk.gormkrings.statistics.StatisticsService;
import dk.gormkrings.tax.ITaxExemption;
import dk.gormkrings.tax.ITaxExemptionFactory;
import dk.gormkrings.tax.ITaxRule;
import dk.gormkrings.tax.ITaxRuleFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SimulationRunner {

    private final ISimulationFactory simulationFactory;
    private final IDateFactory dateFactory;
    private final IPhaseFactory phaseFactory;
    private final ISpecificationFactory specificationFactory;
    private final SimulationAggregationService aggregationService;
    private final ITaxRuleFactory taxRuleFactory;
    private final ITaxExemptionFactory taxExemptionFactory;
    private final IActionFactory actionFactory;
    private final StatisticsService statisticsService;

    /**
     * Runs a simulation and persists summaries/grids.
     *
     * @param simulationId logical ID for this run (used for persistence / SSE correlation)
     * @param request      input payload from the API
     * @param runs         number of Monte Carlo runs
     * @param batchSize    batch size used by the engine
     * @param onProgress   callback for progress / log messages
     */
    public List<IRunResult> runSimulation(
            String simulationId,
            SimulationRequest request,
            int runs,
            int batchSize,
            IProgressCallback onProgress) {

        // Preserve existing behavior for the legacy endpoint:
        //  - returnType: dataDrivenReturn
        //  - inflationFactor: 1.02
        var spec = new SimulationRunSpec(
                request.getStartDate(),
                request.getPhases(),
                request.getOverallTaxRule() == null ? null : request.getOverallTaxRule().toFactoryKey(),
                request.getTaxPercentage(),
                "dataDrivenReturn",
                1.02D
        );
        return runSimulation(simulationId, spec, request, runs, batchSize, onProgress);
    }

    /**
     * Runs a simulation based on an internal run spec, and persists using the provided input object.
     *
     * @param inputForStorage Object persisted for dedup/hash and later retrieval. For legacy flows this
     *                        should be the original request DTO to keep behavior unchanged.
     * @param resolvedAdvanced Optional resolved AdvancedSimulationRequest with all defaults applied.
     *                          Will be persisted as resolvedInputJson for frontend transparency.
     */
    public List<IRunResult> runSimulation(
            String simulationId,
            SimulationRunSpec spec,
            Object inputForStorage,
            Object resolvedAdvanced,
            int runs,
            int batchSize,
            IProgressCallback onProgress) {
        return runSimulationOutcome(
                simulationId,
                spec,
                inputForStorage,
                resolvedAdvanced,
                runs,
                batchSize,
                true,
                onProgress
        ).results();
    }

        public record RunTimings(long computeMs, long aggregateMs, long gridsMs, long persistMs, long totalMs) {
        }

        public record RunOutcome(List<IRunResult> results, List<dk.gormkrings.statistics.YearlySummary> summaries, Long rngSeed, RunTimings timings) {
    }

    /**
     * Runs a simulation and returns both the raw results and the aggregated summaries.
     *
     * @param persistToDb when false, skips inserting the run/summaries into the DB (used for random runs).
     */
    public RunOutcome runSimulationOutcome(
            String simulationId,
            SimulationRunSpec spec,
            Object inputForStorage,
            Object resolvedAdvanced,
            int runs,
            int batchSize,
            boolean persistToDb,
            IProgressCallback onProgress) {
        final long t0 = System.nanoTime();

        final long tCompute0 = System.nanoTime();
        var simulationResults = computeSimulationResults(spec, runs, batchSize, onProgress);
        final long tCompute1 = System.nanoTime();

        // Aggregate + grids
        final long tAgg0 = System.nanoTime();
        var summaries = aggregationService.aggregateResults(
                simulationResults,
                simulationId,
                onProgress
        );
        final long tAgg1 = System.nanoTime();

        final long tGrids0 = System.nanoTime();
        var grids = aggregationService.buildPercentileGrids(simulationResults);
        final long tGrids1 = System.nanoTime();

        // Persist run + summaries using the normalized seed (always positive at request mapping).
        Long rngSeed = (spec.getReturnerConfig() == null) ? null : spec.getReturnerConfig().getSeed();
        long persistMs = 0L;
        if (persistToDb && rngSeed != null) {
            final long tPersist0 = System.nanoTime();
            statisticsService.insertNewRunWithSummaries(
                    simulationId,
                    inputForStorage,
                    SimulationSignature.of(runs, batchSize, inputForStorage),
                    resolvedAdvanced,
                    summaries,
                    grids,
                    rngSeed
            );
            final long tPersist1 = System.nanoTime();
            persistMs = TimeUnit.NANOSECONDS.toMillis(tPersist1 - tPersist0);
        }

        final long t1 = System.nanoTime();
        final long computeMs = TimeUnit.NANOSECONDS.toMillis(tCompute1 - tCompute0);
        final long aggregateMs = TimeUnit.NANOSECONDS.toMillis(tAgg1 - tAgg0);
        final long gridsMs = TimeUnit.NANOSECONDS.toMillis(tGrids1 - tGrids0);
        final long totalMs = TimeUnit.NANOSECONDS.toMillis(t1 - t0);

                if (persistToDb && rngSeed != null) {
                        try {
                                statisticsService.updateRunTimings(simulationId, computeMs, aggregateMs, gridsMs, persistMs, totalMs);
                        } catch (Exception ignore) {
                                // best-effort
                        }
                }

        return new RunOutcome(simulationResults, summaries, rngSeed, new RunTimings(computeMs, aggregateMs, gridsMs, persistMs, totalMs));
    }

    /**
     * Legacy overload without resolved request parameter.
     */
    public List<IRunResult> runSimulation(
            String simulationId,
            SimulationRunSpec spec,
            Object inputForStorage,
            int runs,
            int batchSize,
            IProgressCallback onProgress) {
        return runSimulation(simulationId, spec, inputForStorage, null, runs, batchSize, onProgress);
    }

        /**
         * Computes the full simulation results (snapshots) without persisting anything.
         * Useful for export flows where the run already exists in the DB (dedup hit) but
         * we still want to generate a full per-run CSV.
         */
        public List<IRunResult> runSimulationNoPersist(
                        SimulationRunSpec spec,
                        int runs,
                        int batchSize,
                        IProgressCallback onProgress) {
                return computeSimulationResults(spec, runs, batchSize, onProgress);
        }

        private List<IRunResult> computeSimulationResults(
                        SimulationRunSpec spec,
                        int runs,
                        int batchSize,
                        IProgressCallback onProgress) {

                // Overall tax rule for this run
                ITaxRule overAllTaxRule = taxRuleFactory.create(spec.getOverallTaxRule(), spec.getTaxPercentage());

                var specification = specificationFactory.create(
                                spec.getEpochDay(),
                                overAllTaxRule,
                                spec.getReturnType(),
                                spec.getInflationFactor(),
                                spec.getYearlyFeePercentage(),
                                spec.getReturnerConfig()
                );

                // Build phases
                var currentDate = dateFactory.dateOf(
                                spec.getStartDate().getYear(),
                                spec.getStartDate().getMonth(),
                                spec.getStartDate().getDayOfMonth()
                );

                List<IPhase> phases = new LinkedList<>();
                for (PhaseRequest pr : spec.getPhases()) {
                        List<ITaxExemption> taxExemptions = new LinkedList<>();
                        if (pr.getTaxRules() != null) {
                                for (dk.gormkrings.dto.TaxExemptionRule taxExemption : pr.getTaxRules()) {
                                        if (taxExemption == null) continue;
                                        taxExemptions.add(taxExemptionFactory.create(taxExemption.toFactoryKey(), spec.getTaxExemptionConfig()));
                                }
                        }

                        long days = currentDate.daysUntil(currentDate.plusMonths(pr.getDurationInMonths()));
                        String phaseType = pr.getPhaseType().toFactoryKey();

                        IAction action = switch (phaseType) {
                                case "deposit" -> actionFactory.createDepositAction(
                                                pr.getInitialDeposit(),
                                                pr.getMonthlyDeposit(),
                                                pr.getYearlyIncreaseInPercentage()
                                );
                                case "withdraw" -> actionFactory.createWithdrawAction(
                                                pr.getWithdrawAmount(),
                                                pr.getWithdrawRate(),
                                                pr.getLowerVariationPercentage(),
                                                pr.getUpperVariationPercentage()
                                );
                                case "passive" -> actionFactory.createPassiveAction();
                                default -> throw new IllegalArgumentException("Unknown phase type: " + phaseType);
                        };

                        phases.add(phaseFactory.create(
                                        phaseType,
                                        specification,
                                        currentDate,
                                        taxExemptions,
                                        days,
                                        action
                        ));
                        currentDate = currentDate.plusMonths(pr.getDurationInMonths());
                }

                // Run Monte Carlo with progress callback
                return simulationFactory.createSimulation()
                                .runWithProgress(runs, batchSize, phases, onProgress);
        }
}
