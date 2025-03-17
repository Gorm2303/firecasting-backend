package dk.gormkrings;

import dk.gormkrings.simulation.LiveData;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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
        System.out.println("Application started");
        // For example, simulate 730 days (2 years)
        LiveData data = new LiveData(730, 1000);
        simulationEngine.runSimulation(data);
    }
}
