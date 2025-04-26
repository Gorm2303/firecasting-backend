package dk.gormkrings.specification;

import dk.gormkrings.data.ILive;
import dk.gormkrings.inflation.IInflation;
import dk.gormkrings.returns.IReturner;
import dk.gormkrings.tax.ITaxRule;

public interface ISpecification {
    ISpecification copy();
    ILive getLiveData();
    IReturner getReturner();
    IInflation getInflation();
}
