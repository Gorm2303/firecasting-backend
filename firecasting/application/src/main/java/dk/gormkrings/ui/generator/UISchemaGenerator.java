package dk.gormkrings.ui.generator;

import dk.gormkrings.annotations.UIField;
import dk.gormkrings.ui.fields.UISchemaField;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class UISchemaGenerator {

    public static List<UISchemaField> generateSchema(Class<?> clazz) {
        List<UISchemaField> fields = new ArrayList<>();

        for (Field field : clazz.getDeclaredFields()) {
            UIField annotation = field.getAnnotation(UIField.class);
            if (annotation != null) {
                fields.add(new UISchemaField(
                        field.getName(),
                        annotation.label(),
                        annotation.type(),
                        annotation.required(),
                        List.of(annotation.options())
                ));
            }
        }

        return fields;
    }
}
