package dk.gormkrings.simulation.simulations;

import dk.gormkrings.simulation.data.Result;
import dk.gormkrings.simulation.phases.normal.Phase;

import java.util.List;

public interface Simulation {
    List<Result> run(long instances, List<Phase> phases);
}
