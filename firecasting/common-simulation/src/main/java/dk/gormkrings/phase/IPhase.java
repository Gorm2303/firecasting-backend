package dk.gormkrings.phase;

import dk.gormkrings.data.IDate;
import dk.gormkrings.data.ILive;
import dk.gormkrings.specification.ISpec;

public interface IPhase {
    IDate getStartDate();
    long getDuration();
    IPhase copy(ISpec specificationCopy);
    ISpec getSpecification();
    ILive getLiveData();
}
