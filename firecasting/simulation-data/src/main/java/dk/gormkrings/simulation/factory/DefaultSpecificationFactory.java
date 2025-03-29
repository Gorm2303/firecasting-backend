package dk.gormkrings.simulation.factory;

import dk.gormkrings.inflation.IInflationFactory;
import dk.gormkrings.returns.IReturnFactory;
import dk.gormkrings.factory.ISpecificationFactory;
import dk.gormkrings.tax.ITaxRuleFactory;
import dk.gormkrings.simulation.specification.Specification;
import dk.gormkrings.specification.ISpecification;
import org.springframework.stereotype.Component;

@Component
public class DefaultSpecificationFactory implements ISpecificationFactory {
    private IInflationFactory inflationFactory;
    private ITaxRuleFactory taxRuleFactory;
    private IReturnFactory returnFactory;

    public DefaultSpecificationFactory(IInflationFactory inflationFactory, ITaxRuleFactory taxRuleFactory, IReturnFactory returnFactory) {
        this.inflationFactory = inflationFactory;
        this.taxRuleFactory = taxRuleFactory;
        this.returnFactory = returnFactory;
    }

    @Override
    public ISpecification newSpecification(long startTime, float taxRule, float returnPercentage) {
        return new Specification(startTime, taxRuleFactory.createTaxRule(taxRule), returnFactory.createReturn(returnPercentage), inflationFactory.createInflation());
    }

    @Override
    public ISpecification newSpecification(long startTime, float taxRule, float returnPercentage, float inflation) {
        return new Specification(startTime, taxRuleFactory.createTaxRule(taxRule), returnFactory.createReturn(returnPercentage), inflationFactory.createInflation(inflation));
    }
}
