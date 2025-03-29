package dk.gormkrings;

import dk.gormkrings.action.Deposit;
import dk.gormkrings.action.Passive;
import dk.gormkrings.action.Withdraw;
import dk.gormkrings.data.IDate;
import dk.gormkrings.factory.IDateFactory;
import dk.gormkrings.inflation.Inflation;
import dk.gormkrings.inflation.DataAverageInflation;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.phase.callBased.DepositCallPhase;
import dk.gormkrings.phase.callBased.PassiveCallPhase;
import dk.gormkrings.phase.callBased.WithdrawCallPhase;
import dk.gormkrings.phase.eventBased.DepositEventPhase;
import dk.gormkrings.phase.eventBased.PassiveEventPhase;
import dk.gormkrings.phase.eventBased.WithdrawEventPhase;
import dk.gormkrings.result.IResult;
import dk.gormkrings.returns.Return;
import dk.gormkrings.returns.SimpleMonthlyReturn;
import dk.gormkrings.simulation.ISimulation;
import dk.gormkrings.simulation.specification.Specification;
import dk.gormkrings.simulation.util.CsvExporter;
import dk.gormkrings.simulation.util.Formatter;
import dk.gormkrings.specification.ISpec;
import dk.gormkrings.tax.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.LinkedList;
import java.util.List;

@Slf4j
@SpringBootApplication(scanBasePackages = "dk.gormkrings")
public class FirecastingApplication implements CommandLineRunner {

    private final ISimulation simulation;
    private final IDateFactory dateFactory;

    public FirecastingApplication(@Qualifier("scheduleMCSimulation") ISimulation simulation, IDateFactory dateFactory) {
        this.simulation = simulation;
        this.dateFactory = dateFactory;
    }

    public static void main(String[] args) {
        SpringApplication.run(FirecastingApplication.class, args);
    }

    @Override
    public void run(String... args) {
        Formatter.debug = false;
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

        ISpec specification = createSpecification(depositStartIDate.getEpochDay());

        Deposit deposit = new Deposit(10000, 5000);
        Passive passive = new Passive();
        Withdraw withdraw = new Withdraw(0, 0.04);

        IPhase currentPhase = new DepositCallPhase(specification, depositStartIDate, depositDays, deposit);
        phases.add(currentPhase);

        currentPhase = new PassiveCallPhase(specification, passiveStartIDate, passiveDays, passive);
        phases.add(currentPhase);

        currentPhase = new WithdrawCallPhase(specification, withdrawStartIDate, withdrawDays, withdraw);
        phases.add(currentPhase);

        long startTime = System.currentTimeMillis();
        List<IResult> results = simulation.run(1, phases);
        long simTime = System.currentTimeMillis();
        CsvExporter.exportResultsToCsv(results, "firecasting-results.csv");
        long exportTime = System.currentTimeMillis();

        log.info("Handling runs in {} ms", simTime - startTime);
        log.info("Handling exports in {} ms", exportTime - simTime);
        log.info("Elapsed time: {} seconds", ((double) (System.currentTimeMillis() - startTime)) / 1000);
        log.info("Application Ended");
    }

    private static Specification createSpecification(long startTime) {
        NotionalGainsTax taxation = new NotionalGainsTax(42);
        StockExemptionTax stockExemptionTax = null;
        if (stockExemptionTax != null) taxation.setStockExemptionTax(stockExemptionTax);
        TaxExemptionCard taxExemptionCard = null;
        if (taxExemptionCard != null) taxation.setTaxExemptionCard(taxExemptionCard);

        Return basicReturn = new SimpleMonthlyReturn(7);
        Inflation inflation = new DataAverageInflation();
        return new Specification(startTime, taxation, basicReturn, inflation);
    }
}
