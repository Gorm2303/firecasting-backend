package dk.gormkrings;

import dk.gormkrings.action.Deposit;
import dk.gormkrings.action.Withdraw;
import dk.gormkrings.data.LiveData;
import dk.gormkrings.simulation.data.Result;
import dk.gormkrings.simulation.phases.PassivePhase;
import dk.gormkrings.simulation.phases.DepositPhase;
import dk.gormkrings.simulation.phases.Phase;
import dk.gormkrings.simulation.phases.WithdrawPhase;
import dk.gormkrings.simulation.simulations.MonteCarloSimulation;
import dk.gormkrings.taxes.NotionalGainsTax;
import dk.gormkrings.taxes.TaxRule;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication(scanBasePackages = "dk.gormkrings")
public class FirecastingApplication implements CommandLineRunner {

    private MonteCarloSimulation simulation;

    public FirecastingApplication(MonteCarloSimulation simulation) {
        this.simulation = simulation;
    }

    public static void main(String[] args) {
        SpringApplication.run(FirecastingApplication.class, args);
    }

    @Override
    public void run(String... args) {
        List<Phase> phases = new ArrayList<>();
        System.out.println("Application Started");

        LiveData liveData = new LiveData();
        LocalDate startDate = LocalDate.of(2025,1,1);
        Deposit deposit = new Deposit(10000, 5000);
        TaxRule notionalTax = new NotionalGainsTax();
        Withdraw withdraw = new Withdraw(0.04F);

        int depositDurationInMonths = 10 *12;
        long days = getDurationInDays(startDate,depositDurationInMonths);
        Phase currentPhase = new DepositPhase(startDate, days, deposit, liveData, notionalTax);
        phases.add(currentPhase);

        int passiveDurationInMonths = 5 *12;
        startDate = getNewStartDate(startDate, getDurationInDays(startDate, depositDurationInMonths));
        days = getDurationInDays(startDate, passiveDurationInMonths);
        currentPhase = new PassivePhase(currentPhase, startDate, days, notionalTax);
        phases.add(currentPhase);

        int withdrawDurationInMonths = 30 *12;
        startDate = getNewStartDate(startDate, getDurationInDays(startDate, withdrawDurationInMonths));
        days = getDurationInDays(startDate, withdrawDurationInMonths);
        currentPhase = new WithdrawPhase(currentPhase, startDate, days, withdraw, notionalTax);
        phases.add(currentPhase);

        long startTime = System.currentTimeMillis();

        List<Result> results = simulation.runMonteCarlo(10000, phases);
        System.out.println("These are the results");
        for (Result result : results) {
            System.out.println("Result: ");
            result.print();
        }
        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        System.out.println("Elapsed time: " + ((double) elapsedTime)/1000 + " seconds");
        System.out.println("Application Ended");
    }

    private LocalDate getNewStartDate(LocalDate oldStartDate, long durationInDays) {
        return oldStartDate.plusDays(durationInDays);
    }

    private long getDurationInDays(LocalDate startDate, long months) {
        LocalDate endDate = startDate.plusMonths(months);
        return startDate.until(endDate, ChronoUnit.DAYS);
    }
}
