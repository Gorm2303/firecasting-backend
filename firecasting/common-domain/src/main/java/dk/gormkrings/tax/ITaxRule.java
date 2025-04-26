package dk.gormkrings.tax;

public interface ITaxRule {
    double calculateTax(double amount);
    void yearlyUpdate();
    ITaxRule copy();

}
