package dk.gormkrings.simulation.phases;

import dk.gormkrings.data.Live;
import dk.gormkrings.simulation.specification.Spec;
import org.springframework.context.event.SmartApplicationListener;

import java.time.LocalDate;

public interface Phase extends SmartApplicationListener {
    LocalDate getStartDate();
    long getDuration();
    String getName();
    LocalDate getCurrentLocalDate();
    Phase copy(Spec specificationCopy);
    void addReturn();
    Spec getSpecification();
    Live getLiveData();

}
