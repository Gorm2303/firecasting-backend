package dk.gormkrings.simulation;

import dk.gormkrings.phase.IPhase;
import dk.gormkrings.result.IRunResult;

import java.util.List;

public interface ISimulation {
    List<IRunResult> run(long instances, List<IPhase> phases);
    List<IRunResult> runWithProgress(long runs, List<IPhase> phases, IProgressCallback callback);
}
