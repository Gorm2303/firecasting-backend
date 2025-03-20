package dk.gormkrings;

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
        Phase currentPhase = new DepositPhase(startDate, depositDurationInMonths, deposit, liveData, notionalTax);
        phases.add(currentPhase);

        int passiveDurationInMonths = 5 *12;
        startDate = getNewStartDate(startDate, getDurationInDays(startDate, depositDurationInMonths));
        long days = getDurationInDays(startDate, passiveDurationInMonths);
        currentPhase = new PassivePhase(currentPhase, startDate, days, notionalTax);
        phases.add(currentPhase);

        int withdrawDurationInMonths = 30 *12;
        currentPhase = new WithdrawPhase(currentPhase, withdrawDurationInMonths, withdraw, notionalTax);
        phases.add(currentPhase);

        List<Result> results = simulation.runMonteCarlo(100, phases);
        System.out.println("These are the results");
        for (Result result : results) {
            System.out.println("Result: ");
            result.print();
        }
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
