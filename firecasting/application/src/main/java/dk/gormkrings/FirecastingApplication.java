package dk.gormkrings;

import dk.gormkrings.data.LiveData;
import dk.gormkrings.simulation.Engine;
import dk.gormkrings.simulation.phases.BreakPhase;
import dk.gormkrings.simulation.phases.DepositPhase;
import dk.gormkrings.simulation.phases.Phase;
import dk.gormkrings.simulation.phases.WithdrawPhase;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication(scanBasePackages = "dk.gormkrings")
public class FirecastingApplication implements CommandLineRunner {

    private final Engine simulationEngine;

    public FirecastingApplication(Engine simulationEngine) {
        this.simulationEngine = simulationEngine;
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
        int durationInYears = 10;
        Deposit deposit = new Deposit(10000, 5000);

        Phase currentPhase = makeDepositPhase(startDate, durationInYears, deposit, liveData);
    }

    private Phase makeDepositPhase(LocalDate startDate, int durationInYears, Deposit deposit, LiveData liveData) {
        System.out.println("Initializing Deposit Phase");

        LocalDate endDate = startDate.plusYears(durationInYears);
        int days = (int) startDate.until(endDate, ChronoUnit.DAYS);

        DepositPhase depositPhase = new DepositPhase();
        depositPhase.setStartDate(startDate);
        depositPhase.setDeposit(deposit);
        depositPhase.setDuration(days);
        depositPhase.setLiveData(liveData);

        return simulationEngine.runSimulation(depositPhase);
    }

    private Phase makeBreakPhase(Phase phase) {
        return new BreakPhase();
    }

    private Phase makeWithdrawPhase(Phase phase) {
        return new WithdrawPhase();
    }

}
