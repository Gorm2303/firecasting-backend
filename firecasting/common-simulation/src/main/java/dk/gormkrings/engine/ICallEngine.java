package dk.gormkrings.engine;

import dk.gormkrings.phase.ICallPhase;
import dk.gormkrings.result.IResult;

import java.util.List;

public interface ICallEngine {
    IResult simulatePhases(List<ICallPhase> phaseCopies);
}
