package dk.gormkrings.engine;

import dk.gormkrings.phase.IPhase;
import dk.gormkrings.result.IRunResult;

import java.util.List;

public interface IEngine {
    IRunResult simulatePhases(List<IPhase> phaseCopies);
    void init(List<IPhase> phases);
    }
