package dk.gormkrings;

import dk.gormkrings.action.Deposit;
import dk.gormkrings.action.IAction;
import dk.gormkrings.action.Passive;
import dk.gormkrings.action.Withdraw;
import dk.gormkrings.dto.PhaseRequest;
import dk.gormkrings.dto.SimulationRequest;
import dk.gormkrings.factory.*;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.result.IResult;
import dk.gormkrings.simulation.util.ConcurrentCsvExporter;
import dk.gormkrings.simulation.util.Formatter;
import dk.gormkrings.statistics.SimulationAggregationService;
import dk.gormkrings.statistics.YearlySummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;

@Slf4j
@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/simulation")
public class FirecastingController {

    private final ISimulationFactory simulationFactory;
    private final IDateFactory dateFactory;
    private final IDepositPhaseFactory depositPhaseFactory;
    private final IPassivePhaseFactory passivePhaseFactory;
    private final IWithdrawPhaseFactory withdrawPhaseFactory;
    private final ISpecificationFactory specificationFactory;
    private final SimulationAggregationService aggregationService;

    @Value("${settings.runs}")
    private int runs;

    // Field to store the simulation results for later export.
    private List<IResult> lastResults;

    public FirecastingController(ISimulationFactory simulationFactory,
                                 IDateFactory dateFactory,
                                 IDepositPhaseFactory depositPhaseFactory,
                                 IPassivePhaseFactory passivePhaseFactory,
                                 IWithdrawPhaseFactory withdrawPhaseFactory,
                                 ISpecificationFactory specificationFactory,
                                 SimulationAggregationService aggregationService) {
        this.simulationFactory = simulationFactory;
        this.dateFactory = dateFactory;
        this.depositPhaseFactory = depositPhaseFactory;
        this.passivePhaseFactory = passivePhaseFactory;
        this.withdrawPhaseFactory = withdrawPhaseFactory;
        this.specificationFactory = specificationFactory;
        this.aggregationService = aggregationService;
    }

    @PostMapping
    public ResponseEntity<List<YearlySummary>> runSimulation(@RequestBody SimulationRequest request) {
        var specification = specificationFactory.newSpecification(
                request.getEpochDay(), request.getTaxPercentage(), request.getReturnPercentage(), 2f);

        var currentDate = dateFactory.dateOf(
                request.getStartDate().getYear(),
                request.getStartDate().getMonth(),
                request.getStartDate().getDayOfMonth());

        List<IPhase> phases = new LinkedList<>();

        for (PhaseRequest pr : request.getPhases()) {
            long days = currentDate.daysUntil(currentDate.plusMonths(pr.getDurationInMonths()));
            IPhase phase = switch (pr.getPhaseType().toUpperCase()) {
                case "DEPOSIT" -> {
                    IAction deposit = new Deposit(pr.getInitialDeposit(), pr.getMonthlyDeposit());
                    yield depositPhaseFactory.createDepositPhase(specification, currentDate, days, deposit);
                }
                case "PASSIVE" -> {
                    IAction passive = new Passive();
                    yield passivePhaseFactory.createPassivePhase(specification, currentDate, days, passive);
                }
                case "WITHDRAW" -> {
                    double withdrawAmount = pr.getWithdrawAmount() != null ? pr.getWithdrawAmount() : 0;
                    IAction withdraw = new Withdraw(withdrawAmount, pr.getWithdrawRate(), 0.5);
                    yield withdrawPhaseFactory.createWithdrawPhase(specification, currentDate, days, withdraw);
                }
                default -> throw new IllegalArgumentException("Unknown phase type: " + pr.getPhaseType());
            };
            phases.add(phase);
            currentDate = currentDate.plusMonths(pr.getDurationInMonths());
        }

        long startTime = System.currentTimeMillis();
        lastResults = simulationFactory.createSimulation().run(runs, phases);
        long simTime = System.currentTimeMillis();
        log.info("Handling runs in {} ms", simTime - startTime);

        startTime = System.currentTimeMillis();
        List<YearlySummary> statistics = aggregationService.aggregateResults(lastResults);
        long aggregationTime = System.currentTimeMillis();
        log.info("Handling aggregating results in {} ms", aggregationTime - startTime);

        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/export")
    public ResponseEntity<StreamingResponseBody> exportResultsAsCsv() {
        if (lastResults == null || lastResults.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        long simTime = System.currentTimeMillis();
        // Use your ConcurrentCsvExporter to write the CSV file.
        File file;
        try {
            file = ConcurrentCsvExporter.exportCsv(lastResults, "simulation-results");
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(outputStream -> {
                outputStream.write("Error exporting CSV".getBytes());
            });
        }
        long exportTime = System.currentTimeMillis();
        log.info("Handling exports in {} ms", exportTime - simTime);

        // Read the generated CSV file and stream its contents.
        StreamingResponseBody stream = out -> {
            Files.copy(file.toPath(), out);
            out.flush();
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(stream);
    }
}
