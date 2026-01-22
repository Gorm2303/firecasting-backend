package dk.gormkrings.api;

import java.util.Collections;
import java.util.List;

/**
 * Indicates a client-side request validation failure with structured details.
 *
 * This is intentionally a RuntimeException so it can be thrown from mapping/validation layers
 * and handled centrally by {@link ApiExceptionHandler}.
 */
public class ApiValidationException extends RuntimeException {

    private final List<String> details;

    public ApiValidationException(String message, List<String> details) {
        super(message);
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public ApiValidationException(String message, List<String> details, Throwable cause) {
        super(message, cause);
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public List<String> getDetails() {
        return Collections.unmodifiableList(details);
    }
}
