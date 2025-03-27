package dk.gormkrings.tax;

public interface TaxRule {
    double calculateTax(double amount);
    TaxRule copy();

}
