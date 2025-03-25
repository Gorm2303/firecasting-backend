package dk.gormkrings.simulation.phases;

import dk.gormkrings.data.Live;
import dk.gormkrings.simulation.specification.Spec;
import org.springframework.context.event.SmartApplicationListener;

import java.time.LocalDate;

public interface Phase extends SmartApplicationListener {
    LocalDate getStartDate();
    long getDuration();
    Phase copy(Spec specificationCopy);
    Spec getSpecification();
    Live getLiveData();

}
