package dk.gormkrings.factory;

import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.ITaxRule;
import dk.gormkrings.returns.ReturnerConfig;

public interface ISpecificationFactory {
    ISpecification create(long startTime, ITaxRule taxRule, String returnType);
    ISpecification create(long startTime, ITaxRule taxRule, String returnType, double inflation);

    /**
     * Advanced-mode overload.
     * Implementations may ignore {@code returnerConfig} and fall back to defaults.
     */
    default ISpecification create(
            long startTime,
            ITaxRule taxRule,
            String returnType,
            double inflation,
            ReturnerConfig returnerConfig) {
        return create(startTime, taxRule, returnType, inflation);
    }

}
