package dk.gormkrings.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProgressUpdate {
    public enum Kind { RUNS, MESSAGE }

    private Kind kind;
    private long completed;
    private long total;
    private String message;

    public ProgressUpdate() {} // for Jackson

    private ProgressUpdate(Kind kind, long completed, long total, String message) {
        this.kind = kind; this.completed = completed; this.total = total; this.message = message;
    }

    public static ProgressUpdate runs(long completed, long total) {
        return new ProgressUpdate(Kind.RUNS, completed, total, null);
    }
    public static ProgressUpdate message(String text) {
        return new ProgressUpdate(Kind.MESSAGE, 0, 0, text);
    }

}
