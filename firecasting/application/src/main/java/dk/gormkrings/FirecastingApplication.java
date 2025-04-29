package dk.gormkrings;

import dk.gormkrings.action.IAction;
import dk.gormkrings.action.Deposit;
import dk.gormkrings.action.Passive;
import dk.gormkrings.action.Withdraw;
import dk.gormkrings.data.IDate;
import dk.gormkrings.factory.*;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.result.IRunResult;
import dk.gormkrings.simulation.ISimulation;
import dk.gormkrings.simulation.util.ConcurrentCsvExporter;
import dk.gormkrings.simulation.util.Formatter;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.DefaultTaxExemptionFactory;
import dk.gormkrings.tax.ITaxExemption;
import dk.gormkrings.tax.ITaxRule;
import dk.gormkrings.tax.ITaxRuleFactory;
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
    private final ITaxRuleFactory defaultTaxRuleFactory;
    private final DefaultTaxExemptionFactory defaultTaxExemptionFactory;

    @Value("${settings.run-local}")
    private boolean runLocal = false;
    @Value("${settings.debug}")
    private boolean debug = false;

    public FirecastingApplication(ISimulation simulation,
                                  IDateFactory dateFactory,
                                  IDepositPhaseFactory depositPhaseFactory,
                                  IPassivePhaseFactory passivePhaseFactory,
                                  IWithdrawPhaseFactory withdrawPhaseFactory,
                                  ISpecificationFactory specificationFactory,
                                  ITaxRuleFactory defaultTaxRuleFactory, DefaultTaxExemptionFactory defaultTaxExemptionFactory) {
        this.simulation = simulation;
        this.dateFactory = dateFactory;
        this.depositPhaseFactory = depositPhaseFactory;
        this.passivePhaseFactory = passivePhaseFactory;
        this.withdrawPhaseFactory = withdrawPhaseFactory;
        this.specificationFactory = specificationFactory;
        this.defaultTaxRuleFactory = defaultTaxRuleFactory;
        this.defaultTaxExemptionFactory = defaultTaxExemptionFactory;
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

        IDate depositStartIDate = dateFactory.dateOf(2025, 1, 1);
        IDate passiveStartIDate = depositStartIDate.plusMonths(depositDurationInMonths);
        IDate withdrawStartIDate = passiveStartIDate.plusMonths(passiveDurationInMonths);
        IDate withdrawEndIDate = withdrawStartIDate.plusMonths(withdrawDurationInMonths);

        long depositDays = depositStartIDate.daysUntil(passiveStartIDate);
        long passiveDays = passiveStartIDate.daysUntil(withdrawStartIDate);
        long withdrawDays = withdrawStartIDate.daysUntil(withdrawEndIDate);

        ITaxRule taxRule = defaultTaxRuleFactory.createCapitalTax(42);

        ISpecification specification = specificationFactory.newSpecification(depositStartIDate.getEpochDay(), taxRule, 7);

        IAction deposit = new Deposit(10000, 10000, 0.005);
        IAction passive = new Passive();
        IAction withdraw = new Withdraw(0, 0.04, 0,0);
        List<ITaxExemption> depositTaxRules = new LinkedList<>(List.of(defaultTaxExemptionFactory.createExemptionRule(), defaultTaxExemptionFactory.createStockRule()));
        List<ITaxExemption> passiveTaxRules = new LinkedList<>(List.of(defaultTaxExemptionFactory.createExemptionRule(), defaultTaxExemptionFactory.createStockRule()));
        List<ITaxExemption> withdrawTaxRules = new LinkedList<>(List.of(defaultTaxExemptionFactory.createExemptionRule(), defaultTaxExemptionFactory.createStockRule()));

        IPhase currentPhase = depositPhaseFactory.createDepositPhase(specification, depositStartIDate, depositTaxRules, depositDays, deposit);
        phases.add(currentPhase);

        currentPhase = passivePhaseFactory.createPassivePhase(specification, passiveStartIDate, passiveTaxRules, passiveDays, passive);
        phases.add(currentPhase);

        currentPhase = withdrawPhaseFactory.createWithdrawPhase(specification, withdrawStartIDate, withdrawTaxRules, withdrawDays, withdraw);
        phases.add(currentPhase);

        long startTime = System.currentTimeMillis();
        List<IRunResult> results = simulation.run(10000, phases);
        long simTime = System.currentTimeMillis();
        try {
            ConcurrentCsvExporter.exportCsv(results, "firecasting-results");
        } catch (IOException e) {
            log.error("Failed to export simulation results to CSV", e);
            long exportTime = System.currentTimeMillis();

            log.info("Handling runs in {} ms", simTime - startTime);
            log.info("Handling exports in {} ms", exportTime - simTime);
            log.info("Elapsed time: {} seconds", ((double) (System.currentTimeMillis() - startTime)) / 1000);
            log.info("Application Ended");
        }
    }
}
