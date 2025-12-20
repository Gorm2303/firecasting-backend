package dk.gormkrings.config;

import dk.gormkrings.phase.callBased.SimulationCallPhase;
import dk.gormkrings.phase.eventBased.SimulationEventPhase;
import dk.gormkrings.simulation.ReturnStep;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SimulationReturnStepInitializer {

    private final String configuredStep;

    public SimulationReturnStepInitializer(
            @Value("${simulation.return.step:daily}") String configuredStep
    ) {
        this.configuredStep = configuredStep;
    }

    @PostConstruct
    public void init() {
        ReturnStep step = ReturnStep.fromProperty(configuredStep);
        SimulationCallPhase.configureReturnStep(step);
        SimulationEventPhase.configureReturnStep(step);
        log.info("Simulation return step configured: {} (dt={})", step, step.toDt());
    }
}
