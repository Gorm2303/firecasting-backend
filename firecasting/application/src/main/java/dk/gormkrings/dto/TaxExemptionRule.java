package dk.gormkrings.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Tax exemption rules that can be applied per phase.
 *
 * These map 1:1 to keys understood by {@code DefaultTaxExemptionFactory}.
 */
public enum TaxExemptionRule {
    EXEMPTION_CARD("exemptioncard"),
    STOCK_EXEMPTION("stockexemption");

    private final String factoryKey;

    TaxExemptionRule(String factoryKey) {
        this.factoryKey = factoryKey;
    }

    public String toFactoryKey() {
        return factoryKey;
    }

    @JsonValue
    public String toJson() {
        return factoryKey;
    }

    @JsonCreator
    public static TaxExemptionRule fromJson(String value) {
        if (value == null) return null;
        String v = value.trim();
        if (v.isEmpty()) return null;

        for (TaxExemptionRule r : values()) {
            if (r.factoryKey.equalsIgnoreCase(v) || r.name().equalsIgnoreCase(v)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unsupported tax rule: " + value);
    }
}
