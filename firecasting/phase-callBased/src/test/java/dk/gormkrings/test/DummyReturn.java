package dk.gormkrings.test;

import dk.gormkrings.returns.IReturn;

public class DummyReturn implements IReturn {
    @Override
    public double calculateReturn(double amount) {
        // For testing, return a fixed fraction, for example 10% annualized monthly:
        return (amount * 0.10) / 12;
    }

    @Override
    public IReturn copy() {
        return null;
    }
}
