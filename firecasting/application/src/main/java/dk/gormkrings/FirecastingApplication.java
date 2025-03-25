package dk.gormkrings;

import dk.gormkrings.action.Deposit;
import dk.gormkrings.action.Passive;
import dk.gormkrings.action.Withdraw;
import dk.gormkrings.data.LiveData;
import dk.gormkrings.inflation.Inflation;
import dk.gormkrings.inflation.DataAverageInflation;
import dk.gormkrings.returns.Return;
import dk.gormkrings.returns.SimpleMonthlyReturn;
import dk.gormkrings.simulation.specification.Specification;
import dk.gormkrings.simulation.data.Result;
import dk.gormkrings.simulation.phases.PassivePhase;
import dk.gormkrings.simulation.phases.DepositPhase;
import dk.gormkrings.simulation.phases.Phase;
import dk.gormkrings.simulation.phases.WithdrawPhase;
import dk.gormkrings.simulation.simulations.MonteCarloSimulation;
import dk.gormkrings.taxes.*;
import dk.gormkrings.util.Date;
import dk.gormkrings.util.Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.LinkedList;
import java.util.List;

@Slf4j
@SpringBootApplication(scanBasePackages = "dk.gormkrings")
public class FirecastingApplication implements CommandLineRunner {

    private final MonteCarloSimulation simulation;

    public FirecastingApplication(MonteCarloSimulation simulation) {
        this.simulation = simulation;
    }

    public static void main(String[] args) {
        SpringApplication.run(FirecastingApplication.class, args);
    }

    @Override
    public void run(String... args) {
        Util.debug = false;
        List<Phase> phases = new LinkedList<>();
        log.info("Application Started");

        LiveData liveData = new LiveData();

        int depositDurationInMonths = 20 * 12;
        int passiveDurationInMonths = 5 * 12;
        int withdrawDurationInMonths = 30 * 12;

        Date depositStartDate = Date.of(2025,1,1);
        Date passiveStartDate = depositStartDate.plusMonths(depositDurationInMonths);
        Date withdrawStartDate = passiveStartDate.plusMonths(passiveDurationInMonths);
        Date withdrawEndDate = withdrawStartDate.plusMonths(withdrawDurationInMonths);

        long depositDays = depositStartDate.daysUntil(passiveStartDate);
        long passiveDays = passiveStartDate.daysUntil(withdrawStartDate);
        long withdrawDays = withdrawStartDate.daysUntil(withdrawEndDate);

        Specification specification = createSpecification(liveData);

        Deposit deposit = new Deposit(10000, 5000);
        Passive passive = new Passive();
        Withdraw withdraw = new Withdraw(0, 0.04);

        Phase currentPhase = new DepositPhase(specification, depositStartDate, depositDays, deposit);
        phases.add(currentPhase);

        currentPhase = new PassivePhase(specification, passiveStartDate, passiveDays, passive);
        phases.add(currentPhase);

        currentPhase = new WithdrawPhase(specification, withdrawStartDate, withdrawDays, withdraw);
        phases.add(currentPhase);

        long startTime = System.currentTimeMillis();

        List<Result> results = simulation.runMonteCarlo(100000, phases);
        log.debug("These are the results");
        if (Util.debug) {
            for (Result result : results) {
                log.debug("Result: ");
                result.print();
            }
        }
        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        log.info("Elapsed time: {} seconds", ((double) elapsedTime) / 1000);
        log.info("Application Ended");
    }

    private static Specification createSpecification(LiveData liveData) {
        CapitalGainsTax taxation = new CapitalGainsTax(42);
        StockExemptionTax stockExemptionTax = null;
        if (stockExemptionTax != null) taxation.setStockExemptionTax(stockExemptionTax);
        TaxExemptionCard taxExemptionCard = null;
        if (taxExemptionCard != null) taxation.setTaxExemptionCard(taxExemptionCard);

        Return basicReturn = new SimpleMonthlyReturn(7);
        Inflation inflation = new DataAverageInflation();
        return new Specification(liveData, taxation, basicReturn, inflation);
    }
}
