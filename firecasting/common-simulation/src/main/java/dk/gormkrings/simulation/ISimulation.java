package dk.gormkrings.simulation;

import dk.gormkrings.phase.IPhase;
import dk.gormkrings.result.IResult;

import java.util.List;

public interface ISimulation {
    List<IResult> run(long instances, List<IPhase> phases);
}
