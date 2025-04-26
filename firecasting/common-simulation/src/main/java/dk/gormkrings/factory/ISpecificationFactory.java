package dk.gormkrings.factory;

import dk.gormkrings.specification.ISpecification;

public interface ISpecificationFactory {
    ISpecification newSpecification(long startTime, float returnPercentage);
    ISpecification newSpecification(long startTime, float returnPercentage, float inflation);

}
