package dk.gormkrings.tax;

public interface ITaxExemptionFactory {
    ITaxExemption createExemptionRule();
    ITaxExemption createStockRule();

}
