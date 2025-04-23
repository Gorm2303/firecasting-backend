package dk.gormkrings.phase;

import dk.gormkrings.specification.ISpecification;
import org.springframework.context.event.SmartApplicationListener;

public interface IEventPhase extends IPhase, SmartApplicationListener {
    IEventPhase copy(ISpecification specificationCopy);
}
