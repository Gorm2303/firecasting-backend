package dk.gormkrings.tax;

public interface ITaxExemptionFactory {
    ITaxExemption create(String type);

}
