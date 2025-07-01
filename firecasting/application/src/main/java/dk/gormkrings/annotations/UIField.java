package dk.gormkrings.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UIField {
    String label();
    String type(); // e.g. "text", "number", "dropdown"
    boolean required() default false;
    String[] options() default {};
}

