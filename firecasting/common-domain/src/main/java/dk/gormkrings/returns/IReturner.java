package dk.gormkrings.returns;

public interface IReturner {
    double calculateReturn(double amount);
    IReturner copy();
}
