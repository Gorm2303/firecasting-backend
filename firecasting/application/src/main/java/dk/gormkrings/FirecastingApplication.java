package dk.gormkrings;

import dk.gormkrings.data.LiveData;
import dk.gormkrings.simulation.Engine;
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


    }

    private void runOldSimulations() {
        System.out.println("Starting Deposit Simulation");
        LocalDate startDate = LocalDate.of(2025,1,1);
        LocalDate endDate = startDate.plusYears(10);
        int days = (int) startDate.until(endDate, ChronoUnit.DAYS);
        double initialCapital = 10000;

        simulationEngine.runSimulation(startDate, new LiveData(days, initialCapital));


        System.out.println("Starting Break Simulation");
        startDate = endDate;
        endDate = startDate.plusYears(5);
        days = (int) startDate.until(endDate, ChronoUnit.DAYS);
        initialCapital = 20000;

        simulationEngine.runSimulation(startDate, new LiveData(days, initialCapital));


        System.out.println("Starting Withdraw Simulation");
        startDate = endDate;
        endDate = startDate.plusYears(30);
        days = (int) startDate.until(endDate, ChronoUnit.DAYS);
        initialCapital = 30000;

        simulationEngine.runSimulation(startDate, new LiveData(days, initialCapital));

    }
}
