package dk.gormkrings;

import dk.gormkrings.action.Deposit;
import dk.gormkrings.action.IAction;
import dk.gormkrings.action.Passive;
import dk.gormkrings.action.Withdraw;
import dk.gormkrings.dto.PhaseRequest;
import dk.gormkrings.dto.SimulationRequest;
import dk.gormkrings.factory.IDateFactory;
import dk.gormkrings.factory.IDepositPhaseFactory;
import dk.gormkrings.factory.IPassivePhaseFactory;
import dk.gormkrings.factory.IWithdrawPhaseFactory;
import dk.gormkrings.factory.ISpecificationFactory;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.result.IResult;
import dk.gormkrings.simulation.ISimulation;
import dk.gormkrings.simulation.util.Formatter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedList;
import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/simulation")
public class FirecastingController {

    private final ISimulation simulation;
    private final IDateFactory dateFactory;
    private final IDepositPhaseFactory depositPhaseFactory;
    private final IPassivePhaseFactory passivePhaseFactory;
    private final IWithdrawPhaseFactory withdrawPhaseFactory;
    private final ISpecificationFactory specificationFactory;

    public FirecastingController(ISimulation simulation,
                                 IDateFactory dateFactory,
                                 IDepositPhaseFactory depositPhaseFactory,
                                 IPassivePhaseFactory passivePhaseFactory,
                                 IWithdrawPhaseFactory withdrawPhaseFactory,
                                 ISpecificationFactory specificationFactory) {
        this.simulation = simulation;
        this.dateFactory = dateFactory;
        this.depositPhaseFactory = depositPhaseFactory;
        this.passivePhaseFactory = passivePhaseFactory;
        this.withdrawPhaseFactory = withdrawPhaseFactory;
        this.specificationFactory = specificationFactory;
    }

    @PostMapping
    public ResponseEntity<List<IResult>> runSimulation(@RequestBody SimulationRequest request) {
        Formatter.debug = true;
        var specification = specificationFactory.newSpecification(
                request.getEpochDay(), request.getTaxPercentage(), request.getReturnPercentage());

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
                    IAction withdraw = new Withdraw(0, pr.getWithdrawRate());
                    yield withdrawPhaseFactory.createWithdrawPhase(specification, currentDate, days, withdraw);
                }
                default -> throw new IllegalArgumentException("Unknown phase type: " + pr.getPhaseType());
            };
            phases.add(phase);
            currentDate = currentDate.plusMonths(pr.getDurationInMonths());
        }

        List<IResult> results = simulation.run(1, phases);
        return ResponseEntity.ok(results);
    }
}
