package dk.gormkrings.simulation.specification;

import dk.gormkrings.fee.IYearlyFee;
import dk.gormkrings.inflation.IInflation;
import dk.gormkrings.returns.IReturner;
import dk.gormkrings.simulation.data.LiveData;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.ITaxRule;
import lombok.Getter;

@Getter
public class Specification implements ISpecification {
    private final LiveData liveData;
    private final IReturner returner;
    private final IInflation Inflation;
    private final IYearlyFee yearlyFee;
    private final ITaxRule taxRule;

    private static final IYearlyFee NO_FEE = new IYearlyFee() {
        @Override
        public double calculateFee(double capital) {
            return 0.0;
        }

        @Override
        public IYearlyFee copy() {
            return this;
        }
    };

    public Specification(long startTime, ITaxRule taxRule, IReturner returner, IInflation Inflation) {
        this(startTime, taxRule, returner, Inflation, NO_FEE);
    }

    public Specification(long startTime, ITaxRule taxRule, IReturner returner, IInflation Inflation, IYearlyFee yearlyFee) {
        this.liveData = new LiveData(startTime);
        this.taxRule = taxRule;
        this.returner = returner;
        this.Inflation = Inflation;
        this.yearlyFee = (yearlyFee == null) ? NO_FEE : yearlyFee;
    }

    private Specification(LiveData liveData, ITaxRule taxRule, IReturner returner, IInflation Inflation, IYearlyFee yearlyFee) {
        this.liveData = liveData;
        this.taxRule = taxRule;
        this.returner = returner;
        this.Inflation = Inflation;
        this.yearlyFee = (yearlyFee == null) ? NO_FEE : yearlyFee;
    }

    @Override
    public Specification copy() {
        return new Specification(
                liveData.copy(),
                taxRule.copy(),
                returner.copy(),
                Inflation.copy(),
                yearlyFee.copy());
    }
}
