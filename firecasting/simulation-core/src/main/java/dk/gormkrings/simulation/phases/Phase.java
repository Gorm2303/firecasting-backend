package dk.gormkrings.simulation.phases;

import dk.gormkrings.data.Live;
import dk.gormkrings.simulation.specification.Spec;
import dk.gormkrings.util.Date;
import org.springframework.context.event.SmartApplicationListener;

public interface Phase extends SmartApplicationListener {
    Date getStartDate();
    long getDuration();
    Phase copy(Spec specificationCopy);
    Spec getSpecification();
    Live getLiveData();

}
