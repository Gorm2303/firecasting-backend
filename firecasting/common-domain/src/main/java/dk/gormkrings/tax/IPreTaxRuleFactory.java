package dk.gormkrings.tax;

public interface IPreTaxRuleFactory {
    ITaxRule createExemptionRule();
    ITaxRule createStockRule();

}
