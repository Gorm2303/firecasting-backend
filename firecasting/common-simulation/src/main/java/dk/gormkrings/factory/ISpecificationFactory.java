package dk.gormkrings.factory;

import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.ITaxRule;

public interface ISpecificationFactory {
    ISpecification create(long startTime, ITaxRule taxRule);
    ISpecification create(long startTime, ITaxRule taxRule, float inflation);

}
