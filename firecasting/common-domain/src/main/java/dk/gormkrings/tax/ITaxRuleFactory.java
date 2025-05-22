package dk.gormkrings.tax;

public interface ITaxRuleFactory {
    ITaxRule create(String type, double taxRate);
}
