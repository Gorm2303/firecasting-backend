package dk.gormkrings.phase;

import dk.gormkrings.data.IDate;
import dk.gormkrings.data.ILive;
import dk.gormkrings.specification.ISpecification;

public interface IPhase {
    IDate getStartDate();
    long getDuration();
    IPhase copy(ISpecification specificationCopy);
    ISpecification getSpecification();
    ILive getLiveData();
}
