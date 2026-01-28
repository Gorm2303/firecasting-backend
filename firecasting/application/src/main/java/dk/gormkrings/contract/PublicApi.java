package dk.gormkrings.contract;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker for endpoints that are part of the public HTTP contract.
 *
 * Only endpoints marked with this annotation are included in the OpenAPI snapshot.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Tag(name = "Public API")
public @interface PublicApi {
}
