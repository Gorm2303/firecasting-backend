package dk.gormkrings.simulation;

import java.util.Locale;

/**
 * Controls the simulation time step used for applying returns.
 *
 * <p>DAILY: returns are applied on (trading) days.
 * MONTHLY: returns are applied once per month (at MONTH_END).</p>
 */
public enum ReturnStep {
    DAILY,
    MONTHLY;

    public static ReturnStep fromProperty(String value) {
        if (value == null) return DAILY;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "daily" -> DAILY;
            case "monthly" -> MONTHLY;
            default -> throw new IllegalArgumentException(
                    "Unsupported simulation.return.step: '" + value + "' (expected 'daily' or 'monthly')"
            );
        };
    }

    /**
     * Returns the dt used by distributions that assume annualized parameters.
     */
    public double toDt() {
        return switch (this) {
            case DAILY -> 1.0 / 252.0;
            case MONTHLY -> 1.0 / 12.0;
        };
    }
}
