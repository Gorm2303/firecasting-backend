package dk.gormkrings;

import dk.gormkrings.data.LiveData;
import dk.gormkrings.simulation.Engine;
import dk.gormkrings.simulation.phases.DepositPhase;
import dk.gormkrings.simulation.phases.Phase;
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

        runOldSimulations();
    }

    private void runOldSimulations() {
        System.out.println("Starting Deposit Simulation");
        LocalDate startDate = LocalDate.of(2025,1,1);
        LocalDate endDate = startDate.plusYears(10);
        int days = (int) startDate.until(endDate, ChronoUnit.DAYS);
        Deposit deposit = new Deposit(10000, 5000);

        DepositPhase depositPhase = new DepositPhase();
        depositPhase.setStartDate(startDate);
        depositPhase.setDeposit(deposit);
        depositPhase.setDuration(days);
        depositPhase.setLiveData(new LiveData());

        simulationEngine.runSimulation(depositPhase);
    }
}
