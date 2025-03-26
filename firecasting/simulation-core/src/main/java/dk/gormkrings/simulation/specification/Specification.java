package dk.gormkrings.simulation.specification;

import dk.gormkrings.inflation.Inflation;
import dk.gormkrings.returns.Return;
import dk.gormkrings.simulation.data.LiveData;
import dk.gormkrings.taxes.TaxRule;
import lombok.Getter;

@Getter
public class Specification implements Spec {
    private final LiveData liveData;
    private final TaxRule taxRule;
    private final Return returner;
    private final Inflation inflation;

    public Specification(long startTime, TaxRule taxRule, Return returner, Inflation inflation) {
        this.liveData = new LiveData(startTime);
        this.taxRule = taxRule;
        this.returner = returner;
        this.inflation = inflation;
    }

    private Specification(LiveData liveData, TaxRule taxRule, Return returner, Inflation inflation) {
        this.liveData = liveData;
        this.taxRule = taxRule;
        this.returner = returner;
        this.inflation = inflation;
    }

    @Override
    public Specification copy() {
        return new Specification(
                liveData.copy(),
                taxRule.copy(),
                returner.copy(),
                inflation.copy()
        );
    }
}
