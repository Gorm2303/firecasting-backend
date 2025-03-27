package dk.gormkrings.engine;

import dk.gormkrings.phase.IPhase;
import dk.gormkrings.result.IResult;

import java.util.List;

public interface IEngine {
    IResult simulatePhases(List<IPhase> phaseCopies);
    }
