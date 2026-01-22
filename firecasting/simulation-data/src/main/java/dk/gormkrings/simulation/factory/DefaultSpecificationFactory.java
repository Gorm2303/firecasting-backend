package dk.gormkrings.simulation.factory;

import dk.gormkrings.fee.IYearlyFeeFactory;
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
    private final IYearlyFeeFactory yearlyFeeFactory;
    private final IReturnFactory returnFactory;

    public DefaultSpecificationFactory(IInflationFactory inflationFactory, IYearlyFeeFactory yearlyFeeFactory, IReturnFactory returnFactory) {
        this.inflationFactory = inflationFactory;
        this.yearlyFeeFactory = yearlyFeeFactory;
        this.returnFactory = returnFactory;
    }

    @Override
    public ISpecification create(long startTime, ITaxRule taxRule, String returnType) {
        return new Specification(
                startTime,
                taxRule,
                returnFactory.createReturn(returnType),
                inflationFactory.createInflation(),
                yearlyFeeFactory.createYearlyFee()
        );
    }

    @Override
    public ISpecification create(long startTime, ITaxRule taxRule, String returnType, double inflation) {
        return new Specification(
            startTime,
            taxRule,
            returnFactory.createReturn(returnType),
            inflationFactory.createInflation(inflation),
            yearlyFeeFactory.createYearlyFee()
        );
    }

        @Override
        public ISpecification create(
            long startTime,
            ITaxRule taxRule,
            String returnType,
            double inflation,
            double yearlyFeePercentage,
            ReturnerConfig returnerConfig) {
        return new Specification(
            startTime,
            taxRule,
            returnFactory.createReturn(returnType, returnerConfig),
            inflationFactory.createInflation(inflation),
            yearlyFeeFactory.createYearlyFee(yearlyFeePercentage)
        );
        }

    @Override
    public ISpecification create(
            long startTime,
            ITaxRule taxRule,
            String returnType,
            double inflation,
            ReturnerConfig returnerConfig) {
        return create(startTime, taxRule, returnType, inflation, 0.0, returnerConfig);
    }
}
