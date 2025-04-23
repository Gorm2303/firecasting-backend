package dk.gormkrings.simulation.monteCarlo;

import dk.gormkrings.factory.ISimulationFactory;
import dk.gormkrings.simulation.ISimulation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class SimulationFactory implements ISimulationFactory {
    private final ApplicationContext context;

    @Autowired
    public SimulationFactory(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public ISimulation createSimulation() {
        // Retrieve a new MonteCarloSimulation bean from the Spring context.
        return context.getBean(MonteCarloSimulation.class);
    }
}
