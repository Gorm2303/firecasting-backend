package dk.gormkrings;

import dk.gormkrings.data.LiveData;
import dk.gormkrings.simulation.Engine;
import dk.gormkrings.simulation.phases.PassivePhase;
import dk.gormkrings.simulation.phases.DepositPhase;
import dk.gormkrings.simulation.phases.Phase;
import dk.gormkrings.simulation.phases.WithdrawPhase;
import dk.gormkrings.taxes.NotionalGainsTax;
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
        Deposit deposit = new Deposit(10000, 5000);

        int depositDurationInYears = 10;
        Phase currentPhase = makeDepositPhase(startDate, depositDurationInYears, deposit, liveData);

        int passiveDurationInYears = 5;
        currentPhase = makePassivePhase(currentPhase, passiveDurationInYears);

        Withdraw withdraw = new Withdraw(0.04F);

        int withdrawDurationInYears = 30;
        currentPhase = makeWithdrawPhase(currentPhase, withdrawDurationInYears, withdraw);
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
        depositPhase.setTaxRule(new NotionalGainsTax());

        return simulationEngine.runSimulation(depositPhase);
    }

    private Phase makeDepositPhase(Phase phase, int durationInYears, Deposit deposit) {
        return makeDepositPhase(phase.getStartDate(), durationInYears, deposit, phase.getLiveData());
    }

    private Phase makePassivePhase(Phase phase, int durationInYears) {
        System.out.println("Initializing Passive Phase");

        LocalDate startDate = phase.getStartDate().plusDays(phase.getDuration());
        LocalDate endDate = startDate.plusYears(durationInYears);
        int days = (int) startDate.until(endDate, ChronoUnit.DAYS);

        PassivePhase passivePhase = new PassivePhase();
        passivePhase.setStartDate(startDate);
        passivePhase.setDuration(days);
        passivePhase.setLiveData(phase.getLiveData());
        passivePhase.setTaxRule(new NotionalGainsTax());

        return simulationEngine.runSimulation(passivePhase);
    }

    private Phase makeWithdrawPhase(Phase phase, int durationInYears, Withdraw withdraw) {
        System.out.println("Initializing Withdraw Phase");

        LocalDate startDate = phase.getStartDate().plusDays(phase.getDuration());
        LocalDate endDate = startDate.plusYears(durationInYears);
        int days = (int) startDate.until(endDate, ChronoUnit.DAYS);

        WithdrawPhase withdrawPhase = new WithdrawPhase();
        withdrawPhase.setStartDate(startDate);
        withdrawPhase.setWithdraw(withdraw);
        withdrawPhase.setDuration(days);
        withdrawPhase.setLiveData(phase.getLiveData());
        withdrawPhase.setTaxRule(new NotionalGainsTax());

        return simulationEngine.runSimulation(withdrawPhase);
    }

}
