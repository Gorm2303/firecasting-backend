package dk.gormkrings.test;

import dk.gormkrings.returns.IReturner;

public class DummyReturner implements IReturner {
    @Override
    public double calculateReturn(double amount) {
        // For testing, return a fixed fraction, for example 10% annualized monthly:
        return (amount * 0.10) / 12;
    }

    @Override
    public IReturner copy() {
        return null;
    }
}
