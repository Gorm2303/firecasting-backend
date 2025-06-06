package dk.gormkrings.simulation.factory;

import dk.gormkrings.inflation.IInflationFactory;
import dk.gormkrings.returns.IReturnFactory;
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
    public ISpecification create(long startTime, ITaxRule taxRule) {
        return new Specification(startTime, taxRule, returnFactory.createReturn(), inflationFactory.createInflation());
    }

    @Override
    public ISpecification create(long startTime, ITaxRule taxRule, float inflation) {
        return new Specification(startTime, taxRule, returnFactory.createReturn(), inflationFactory.createInflation(inflation));
    }
}
