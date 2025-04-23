package dk.gormkrings.factory;

import dk.gormkrings.specification.ISpecification;

public interface ISpecificationFactory {
    ISpecification newSpecification(long startTime, float taxRule, float returnPercentage);
    ISpecification newSpecification(long startTime, float taxRule, float returnPercentage, float inflation);

}
