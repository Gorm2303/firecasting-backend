package dk.gormkrings.test;

import dk.gormkrings.data.IDate;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.phase.callBased.SimulationCallPhase;
import dk.gormkrings.specification.ISpecification;

public class TestSimulationCallPhase extends SimulationCallPhase {

    public TestSimulationCallPhase(ISpecification specification, IDate startDate, long duration, String name) {
        super(specification, startDate, duration,name);
    }

    @Override
    public IPhase copy(ISpecification specificationCopy) {
        return null;
    }

    @Override
    public void addReturn() {
        super.addReturn();
    }

    @Override
    public void addTax() {
        super.addTax();
    }

    @Override
    public void addInflation() {
        super.addInflation();
    }
}
