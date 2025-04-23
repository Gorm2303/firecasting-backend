package dk.gormkrings.returns;

import dk.gormkrings.math.randomVariable.IRandomVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class DistributionReturn implements IReturner {
    private final IRandomVariable randomVariable;

    @Autowired
    public DistributionReturn(IRandomVariable randomVariable) {
        this.randomVariable = randomVariable;
    }

    @Override
    public double calculateReturn(double amount) {
        double sample = randomVariable.sample();
        return amount * Math.exp(sample) - amount;
    }

    @Override
    public IReturner copy() {
        return new DistributionReturn(randomVariable.copy());
    }
}