package dk.gormkrings.factory;

import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.ITaxRule;

public interface ISpecificationFactory {
    ISpecification newSpecification(long startTime, ITaxRule taxRule);
    ISpecification newSpecification(long startTime, ITaxRule taxRule, float inflation);

}
