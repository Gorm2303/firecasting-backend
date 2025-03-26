package dk.gormkrings.taxes;

public interface TaxRule {
    double calculateTax(double amount);
    TaxRule copy();

}
