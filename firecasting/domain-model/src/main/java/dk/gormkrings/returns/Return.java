package dk.gormkrings.returns;

public interface Return {
    double calculateReturn(double amount);
    Return copy();
}
