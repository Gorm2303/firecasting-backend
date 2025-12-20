package dk.gormkrings.tax;

public interface ITaxExemptionFactory {
    ITaxExemption create(String type);

    /**
     * Creates a tax exemption with optional configuration overrides.
     * Implementations should fall back to defaults when {@code config} is null.
     */
    ITaxExemption create(String type, TaxExemptionConfig config);

}
