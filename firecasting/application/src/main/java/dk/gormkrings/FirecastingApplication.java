package dk.gormkrings;

import dk.gormkrings.action.Deposit;
import dk.gormkrings.action.Passive;
import dk.gormkrings.action.Withdraw;
import dk.gormkrings.data.LiveData;
import dk.gormkrings.inflation.Inflation;
import dk.gormkrings.inflation.SimpleInflation;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
        List<Phase> phases = new LinkedList<>();
        log.info("Application Started");

        LiveData liveData = new LiveData();

        long depositDurationInMonths = 20 * 12;
        long passiveDurationInMonths = 5 * 12;
        long withdrawDurationInMonths = 30 * 12;

        LocalDate depositStartDate = LocalDate.of(2025,1,1);
        LocalDate passiveStartDate = getNewStartDate(depositStartDate, getDurationInDays(depositStartDate, depositDurationInMonths));
        LocalDate withdrawStartDate = getNewStartDate(passiveStartDate, getDurationInDays(passiveStartDate, passiveDurationInMonths));

        long depositDays = getDurationInDays(depositStartDate, depositDurationInMonths);
        long passiveDays = getDurationInDays(passiveStartDate, passiveDurationInMonths);
        long withdrawDays = getDurationInDays(withdrawStartDate, withdrawDurationInMonths);

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

        List<Result> results = simulation.runMonteCarlo(1, phases);
        log.debug("These are the results");
        for (Result result : results) {
            log.debug("Result: ");
            result.print();
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
        Inflation inflation = new SimpleInflation();
        return new Specification(liveData, taxation, basicReturn, inflation);
    }

    private LocalDate getNewStartDate(LocalDate oldStartDate, long durationInDays) {
        return oldStartDate.plusDays(durationInDays);
    }

    private long getDurationInDays(LocalDate startDate, long months) {
        LocalDate endDate = startDate.plusMonths(months);
        return startDate.until(endDate, ChronoUnit.DAYS);
    }
}
