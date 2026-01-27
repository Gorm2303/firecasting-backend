package dk.gormkrings.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Overall tax rule for the simulation.
 *
 * JSON contract:
 *  - serializes as "Capital" / "Notional" (stable, UI-friendly)
 *  - accepts case-insensitive inputs (e.g. "CAPITAL", "capital", "Capital").
 */
public enum OverallTaxRule {
    CAPITAL("Capital", "capital"),
    NOTIONAL("Notional", "notional");

    private final String jsonValue;
    private final String factoryKey;

    OverallTaxRule(String jsonValue, String factoryKey) {
        this.jsonValue = jsonValue;
        this.factoryKey = factoryKey;
    }

    @JsonValue
    public String toJson() {
        return jsonValue;
    }

    public String toFactoryKey() {
        return factoryKey;
    }

    @JsonCreator
    public static OverallTaxRule fromJson(String value) {
        if (value == null) return null;
        String v = value.trim();
        if (v.isEmpty()) return null;

        for (OverallTaxRule r : values()) {
            if (r.jsonValue.equalsIgnoreCase(v) || r.name().equalsIgnoreCase(v) || r.factoryKey.equalsIgnoreCase(v)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unsupported overallTaxRule: " + value);
    }
}
