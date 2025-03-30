package dk.gormkrings.returns;

public interface IReturn {
    double calculateReturn(double amount);
    IReturn copy();
}
