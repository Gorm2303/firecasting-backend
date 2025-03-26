package dk.gormkrings.simulation.phases;

import dk.gormkrings.data.IDate;
import dk.gormkrings.data.ILive;
import dk.gormkrings.simulation.specification.Spec;


public interface Phase {
    IDate getStartDate();
    long getDuration();
    Phase copy(Spec specificationCopy);
    Spec getSpecification();
    ILive getLiveData();
}
