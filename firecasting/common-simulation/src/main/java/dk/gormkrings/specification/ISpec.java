package dk.gormkrings.specification;

import dk.gormkrings.data.ILive;
import dk.gormkrings.inflation.Inflation;
import dk.gormkrings.returns.Return;
import dk.gormkrings.tax.TaxRule;

public interface ISpec {
    ISpec copy();
    ILive getLiveData();
    TaxRule getTaxRule();
    Return getReturner();
    Inflation getInflation();
}
