package dk.gormkrings.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Phase type in the simulation timeline.
 *
 * JSON contract:
 *  - serializes as "DEPOSIT" / "PASSIVE" / "WITHDRAW"
 *  - accepts case-insensitive inputs.
 */
public enum PhaseType {
    DEPOSIT("deposit"),
    PASSIVE("passive"),
    WITHDRAW("withdraw");

    private final String factoryKey;

    PhaseType(String factoryKey) {
        this.factoryKey = factoryKey;
    }

    /** Key used by the phase factory / runner (lowercase). */
    public String toFactoryKey() {
        return factoryKey;
    }

    @JsonValue
    public String toJson() {
        return name();
    }

    @JsonCreator
    public static PhaseType fromJson(String value) {
        if (value == null) return null;
        String v = value.trim();
        if (v.isEmpty()) return null;

        for (PhaseType t : values()) {
            if (t.name().equalsIgnoreCase(v) || t.factoryKey.equalsIgnoreCase(v)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unsupported phaseType: " + value);
    }
}
