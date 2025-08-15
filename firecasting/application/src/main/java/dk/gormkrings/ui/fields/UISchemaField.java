package dk.gormkrings.ui.fields;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class UISchemaField {
    private String key;
    private String label;
    private String type;
    private boolean required;
    private List<String> options;

}
