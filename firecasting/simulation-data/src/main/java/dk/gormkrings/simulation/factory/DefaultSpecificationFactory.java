package dk.gormkrings.simulation.factory;

import dk.gormkrings.inflation.IInflationFactory;
import dk.gormkrings.returns.IReturnFactory;
import dk.gormkrings.returns.ReturnerConfig;
import dk.gormkrings.factory.ISpecificationFactory;
import dk.gormkrings.tax.ITaxRule;
import dk.gormkrings.simulation.specification.Specification;
import dk.gormkrings.specification.ISpecification;
import org.springframework.stereotype.Component;

@Component
public class DefaultSpecificationFactory implements ISpecificationFactory {
    private final IInflationFactory inflationFactory;
    private final IReturnFactory returnFactory;

    public DefaultSpecificationFactory(IInflationFactory inflationFactory, IReturnFactory returnFactory) {
        this.inflationFactory = inflationFactory;
        this.returnFactory = returnFactory;
    }

    @Override
    public ISpecification create(long startTime, ITaxRule taxRule, String returnType) {
        return new Specification(startTime, taxRule, returnFactory.createReturn(returnType), inflationFactory.createInflation());
    }

    @Override
    public ISpecification create(long startTime, ITaxRule taxRule, String returnType, double inflation) {
        return new Specification(startTime, taxRule, returnFactory.createReturn(returnType), inflationFactory.createInflation(inflation));
    }

    @Override
    public ISpecification create(
            long startTime,
            ITaxRule taxRule,
            String returnType,
            double inflation,
            ReturnerConfig returnerConfig) {
        return new Specification(
                startTime,
                taxRule,
                returnFactory.createReturn(returnType, returnerConfig),
                inflationFactory.createInflation(inflation)
        );
    }
}
