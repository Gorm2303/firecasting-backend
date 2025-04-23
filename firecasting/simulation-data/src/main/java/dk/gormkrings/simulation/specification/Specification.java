package dk.gormkrings.simulation.specification;

import dk.gormkrings.inflation.IInflation;
import dk.gormkrings.returns.IReturner;
import dk.gormkrings.simulation.data.LiveData;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.ITaxRule;
import lombok.Getter;

@Getter
public class Specification implements ISpecification {
    private final LiveData liveData;
    private final ITaxRule taxRule;
    private final IReturner returner;
    private final IInflation Inflation;

    public Specification(long startTime, ITaxRule taxRule, IReturner returner, IInflation Inflation) {
        this.liveData = new LiveData(startTime);
        this.taxRule = taxRule;
        this.returner = returner;
        this.Inflation = Inflation;
    }

    private Specification(LiveData liveData, ITaxRule taxRule, IReturner returner, IInflation Inflation) {
        this.liveData = liveData;
        this.taxRule = taxRule;
        this.returner = returner;
        this.Inflation = Inflation;
    }

    @Override
    public Specification copy() {
        return new Specification(
                liveData.copy(),
                taxRule.copy(),
                returner.copy(),
                Inflation.copy()
        );
    }
}
