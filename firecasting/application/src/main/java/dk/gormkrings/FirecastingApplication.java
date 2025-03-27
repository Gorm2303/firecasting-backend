package dk.gormkrings;

import dk.gormkrings.action.Deposit;
import dk.gormkrings.action.Passive;
import dk.gormkrings.action.Withdraw;
import dk.gormkrings.data.IDate;
import dk.gormkrings.inflation.Inflation;
import dk.gormkrings.inflation.DataAverageInflation;
import dk.gormkrings.returns.Return;
import dk.gormkrings.returns.SimpleMonthlyReturn;
import dk.gormkrings.simulation.data.Date;
import dk.gormkrings.simulation.phases.Phase;
import dk.gormkrings.simulation.simulations.ScheduleMCSimulation;
import dk.gormkrings.simulation.simulations.Simulation;
import dk.gormkrings.simulation.specification.Specification;
import dk.gormkrings.simulation.results.Result;
import dk.gormkrings.simulation.phases.callBased.PassiveCallPhase;
import dk.gormkrings.simulation.phases.callBased.DepositCallPhase;
import dk.gormkrings.simulation.phases.callBased.WithdrawCallPhase;
import dk.gormkrings.simulation.util.CsvExporter;
import dk.gormkrings.simulation.util.Formatter;
import dk.gormkrings.taxes.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.LinkedList;
import java.util.List;

@Slf4j
@SpringBootApplication(scanBasePackages = "dk.gormkrings")
public class FirecastingApplication implements CommandLineRunner {

    private final Simulation simulation;

    public FirecastingApplication(ScheduleMCSimulation simulation) {
        this.simulation = simulation;
    }

    public static void main(String[] args) {
        SpringApplication.run(FirecastingApplication.class, args);
    }

    @Override
    public void run(String... args) {
        Formatter.debug = false;
        List<Phase> phases = new LinkedList<>();
        log.info("Application Started");

        int depositDurationInMonths = 20 * 12;
        int passiveDurationInMonths = 5 * 12;
        int withdrawDurationInMonths = 30 * 12;

        IDate depositStartIDate = new Date(2025,1,1);
        IDate passiveStartIDate = depositStartIDate.plusMonths(depositDurationInMonths);
        IDate withdrawStartIDate = passiveStartIDate.plusMonths(passiveDurationInMonths);
        IDate withdrawEndIDate = withdrawStartIDate.plusMonths(withdrawDurationInMonths);

        long depositDays = depositStartIDate.daysUntil(passiveStartIDate);
        long passiveDays = passiveStartIDate.daysUntil(withdrawStartIDate);
        long withdrawDays = withdrawStartIDate.daysUntil(withdrawEndIDate);

        Specification specification = createSpecification(depositStartIDate.getEpochDay());

        Deposit deposit = new Deposit(10000, 5000);
        Passive passive = new Passive();
        Withdraw withdraw = new Withdraw(0, 0.04);

        Phase currentPhase = new DepositCallPhase(specification, depositStartIDate, depositDays, deposit);
        phases.add(currentPhase);

        currentPhase = new PassiveCallPhase(specification, passiveStartIDate, passiveDays, passive);
        phases.add(currentPhase);

        currentPhase = new WithdrawCallPhase(specification, withdrawStartIDate, withdrawDays, withdraw);
        phases.add(currentPhase);

        long startTime = System.currentTimeMillis();
        List<Result> results = simulation.run(10000, phases);
        long simTime = System.currentTimeMillis();
        CsvExporter.exportResultsToCsv(results, "firecasting-results.csv");
        long exportTime = System.currentTimeMillis();

        log.info("Handling runs in {} ms", simTime - startTime);
        log.info("Handling exports in {} ms", exportTime - simTime);
        log.info("Elapsed time: {} seconds", ((double) (System.currentTimeMillis() - startTime)) / 1000);
        log.info("Application Ended");
    }

    private static Specification createSpecification(long startTime) {
        CapitalGainsTax taxation = new CapitalGainsTax(42);
        StockExemptionTax stockExemptionTax = null;
        if (stockExemptionTax != null) taxation.setStockExemptionTax(stockExemptionTax);
        TaxExemptionCard taxExemptionCard = null;
        if (taxExemptionCard != null) taxation.setTaxExemptionCard(taxExemptionCard);

        Return basicReturn = new SimpleMonthlyReturn(7);
        Inflation inflation = new DataAverageInflation();
        return new Specification(startTime, taxation, basicReturn, inflation);
    }
}
