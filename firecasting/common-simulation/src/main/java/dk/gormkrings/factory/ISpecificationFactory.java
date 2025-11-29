package dk.gormkrings.factory;

import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.ITaxRule;

public interface ISpecificationFactory {
    ISpecification create(long startTime, ITaxRule taxRule, String returnType);
    ISpecification create(long startTime, ITaxRule taxRule, String returnType, double inflation);

}
