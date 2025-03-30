package dk.gormkrings.specification;

import dk.gormkrings.data.ILive;
import dk.gormkrings.inflation.IInflation;
import dk.gormkrings.returns.IReturn;
import dk.gormkrings.tax.ITaxRule;

public interface ISpecification {
    ISpecification copy();
    ILive getLiveData();
    ITaxRule getTaxRule();
    IReturn getReturner();
    IInflation getInflation();
}
