package dk.gormkrings.test;

import dk.gormkrings.inflation.IInflation;
import dk.gormkrings.returns.IReturner;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.ITaxRule;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DummySpecification implements ISpecification {
    private final DummyLiveData liveData = new DummyLiveData();
    private ITaxRule taxRule;
    private IInflation inflation;
    private IReturner returner;

    @Override
    public ISpecification copy() {
        return null;
    }

    @Override
    public IReturner getReturner() {
        return returner;
    }

}
