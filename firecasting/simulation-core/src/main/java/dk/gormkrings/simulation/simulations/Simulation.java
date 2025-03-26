package dk.gormkrings.simulation.simulations;

import dk.gormkrings.simulation.results.Result;
import dk.gormkrings.simulation.phases.Phase;

import java.util.List;

public interface Simulation {
    List<Result> run(long instances, List<Phase> phases);
}
