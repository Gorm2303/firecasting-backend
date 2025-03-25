package dk.gormkrings.simulation.phases.eventbased;

import dk.gormkrings.data.Live;
import dk.gormkrings.simulation.specification.Spec;
import dk.gormkrings.util.Date;
import org.springframework.context.event.SmartApplicationListener;

public interface EPhase extends SmartApplicationListener {
    Date getStartDate();
    long getDuration();
    EPhase copy(Spec specificationCopy);
    Spec getSpecification();
    Live getLiveData();

}
