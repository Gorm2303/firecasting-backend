package dk.gormkrings;

import dk.gormkrings.action.IAction;
import dk.gormkrings.action.Deposit;
import dk.gormkrings.action.Passive;
import dk.gormkrings.action.Withdraw;
import dk.gormkrings.data.IDate;
import dk.gormkrings.factory.*;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.result.IResult;
import dk.gormkrings.simulation.ISimulation;
import dk.gormkrings.simulation.util.ConcurrentCsvExporter;
import dk.gormkrings.simulation.util.Formatter;
import dk.gormkrings.specification.ISpecification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

@Slf4j
@SpringBootApplication(scanBasePackages = "dk.gormkrings")
public class FirecastingApplication implements CommandLineRunner {

    private final ISimulation simulation;
    private final IDateFactory dateFactory;
    private final IDepositPhaseFactory depositPhaseFactory;
    private final IPassivePhaseFactory passivePhaseFactory;
    private final IWithdrawPhaseFactory withdrawPhaseFactory;
    private final ISpecificationFactory specificationFactory;

    @Value("${settings.run-local}")
    private boolean runLocal = false;
    @Value("${settings.debug}")
    private boolean debug = false;

    public FirecastingApplication(ISimulation simulation,
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

    public static void main(String[] args) {
        SpringApplication.run(FirecastingApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        Formatter.debug = debug;
        if (runLocal) {
            runLocal();
        }
    }

    public void runLocal() {
        Formatter.debug = true;
        List<IPhase> phases = new LinkedList<>();
        log.info("Application Started");

        int depositDurationInMonths = 20 * 12;
        int passiveDurationInMonths = 5 * 12;
        int withdrawDurationInMonths = 30 * 12;

        IDate depositStartIDate = dateFactory.dateOf(2025,1,1);
        IDate passiveStartIDate = depositStartIDate.plusMonths(depositDurationInMonths);
        IDate withdrawStartIDate = passiveStartIDate.plusMonths(passiveDurationInMonths);
        IDate withdrawEndIDate = withdrawStartIDate.plusMonths(withdrawDurationInMonths);

        long depositDays = depositStartIDate.daysUntil(passiveStartIDate);
        long passiveDays = passiveStartIDate.daysUntil(withdrawStartIDate);
        long withdrawDays = withdrawStartIDate.daysUntil(withdrawEndIDate);

        ISpecification specification = specificationFactory.newSpecification(depositStartIDate.getEpochDay(), 42, 7);

        IAction deposit = new Deposit(10000, 10000);
        IAction passive = new Passive();
        IAction withdraw = new Withdraw(0, 0.04, 0.5);

        IPhase currentPhase = depositPhaseFactory.createDepositPhase(specification, depositStartIDate, depositDays, deposit);
        phases.add(currentPhase);

        currentPhase = passivePhaseFactory.createPassivePhase(specification, passiveStartIDate, passiveDays, passive);
        phases.add(currentPhase);

        currentPhase = withdrawPhaseFactory.createWithdrawPhase(specification, withdrawStartIDate, withdrawDays, withdraw);
        phases.add(currentPhase);

        long startTime = System.currentTimeMillis();
        List<IResult> results = simulation.run(10000, phases);
        long simTime = System.currentTimeMillis();
        try {
            ConcurrentCsvExporter.exportCsv(results, "firecasting-results");
        } catch (IOException e) {
            e.printStackTrace();
        }
        long exportTime = System.currentTimeMillis();

        log.info("Handling runs in {} ms", simTime - startTime);
        log.info("Handling exports in {} ms", exportTime - simTime);
        log.info("Elapsed time: {} seconds", ((double) (System.currentTimeMillis() - startTime)) / 1000);
        log.info("Application Ended");
    }
}
