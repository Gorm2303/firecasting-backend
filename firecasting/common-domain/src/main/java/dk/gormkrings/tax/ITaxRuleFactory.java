package dk.gormkrings.tax;

public interface ITaxRuleFactory {
    ITaxRule createTaxRule(String taxRule, float taxPercentage);
}
