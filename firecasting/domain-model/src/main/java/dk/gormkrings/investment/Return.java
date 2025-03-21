package dk.gormkrings.investment;

public interface Return {
    double calculateReturn(double amount);
    Return copy();
}
