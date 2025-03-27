package dk.gormkrings.phase;

import dk.gormkrings.specification.ISpec;
import org.springframework.context.event.SmartApplicationListener;

public interface IEventPhase extends IPhase, SmartApplicationListener {
    IEventPhase copy(ISpec specificationCopy);
}
