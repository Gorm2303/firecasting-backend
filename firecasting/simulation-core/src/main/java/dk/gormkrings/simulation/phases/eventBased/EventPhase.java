package dk.gormkrings.simulation.phases.eventBased;

import dk.gormkrings.simulation.phases.Phase;
import dk.gormkrings.simulation.specification.Spec;
import org.springframework.context.event.SmartApplicationListener;

public interface EventPhase extends Phase, SmartApplicationListener {
    EventPhase copy(Spec specificationCopy);
}
