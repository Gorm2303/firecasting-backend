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
                request.getOverallTaxRule(),
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
     */
    public List<IRunResult> runSimulation(
            String simulationId,
            SimulationRunSpec spec,
            Object inputForStorage,
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
                for (String taxExemption : pr.getTaxRules()) {
                                        taxExemptions.add(taxExemptionFactory.create(taxExemption, spec.getTaxExemptionConfig()));
                }
            }

            long days = currentDate.daysUntil(currentDate.plusMonths(pr.getDurationInMonths()));
            String phaseType = pr.getPhaseType().toLowerCase();

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
        var simulationResults = simulationFactory.createSimulation()
                .runWithProgress(runs, batchSize, phases, onProgress);

        // Aggregate + grids
        var summaries = aggregationService.aggregateResults(
                simulationResults,
                simulationId,
                onProgress
        );
        var grids = aggregationService.buildPercentileGrids(simulationResults);

        // Persist run + summaries
        statisticsService.insertNewRunWithSummaries(simulationId, inputForStorage, summaries, grids);

        return simulationResults;
    }
}
