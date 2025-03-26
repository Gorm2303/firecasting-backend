package dk.gormkrings.simulation.engine;

import dk.gormkrings.simulation.data.Result;
import dk.gormkrings.simulation.phases.Phase;

import java.util.List;

public interface Engine {
    Result simulatePhases(List<Phase> phaseCopies);
    }
