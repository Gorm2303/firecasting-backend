package dk.gormkrings.tax;

public interface ITaxRuleFactory {
    ITaxRule createCapitalTax(double taxRate);
    ITaxRule createNotionalTax(double taxRate);
}
