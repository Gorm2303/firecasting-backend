package dk.gormkrings.tax;

public interface ITaxRuleFactory {
    ITaxRule createTaxRule(double taxRate);
}
