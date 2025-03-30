package dk.gormkrings.test;

import dk.gormkrings.data.ILiveData;
import dk.gormkrings.inflation.IInflation;
import dk.gormkrings.returns.IReturn;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.ITaxRule;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DummySpecification implements ISpecification {
    private final DummyLiveData liveData = new DummyLiveData();
    private ITaxRule taxRule;

    @Override
    public ISpecification copy() {
        return null;
    }

    @Override
    public ILiveData getLiveData() {
        return liveData;
    }

    @Override
    public IReturn getReturner() {
        return new DummyReturn();
    }

    @Override
    public IInflation getInflation() {
        return null;
    }

    // Implement other methods if required for your tests.
}
