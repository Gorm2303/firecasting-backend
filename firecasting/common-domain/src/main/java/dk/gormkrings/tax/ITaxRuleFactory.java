package dk.gormkrings.tax;

public interface ITaxRuleFactory {
    TaxRule createTaxRule(double taxRate);
}
