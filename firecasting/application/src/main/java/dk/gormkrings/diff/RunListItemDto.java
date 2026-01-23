package dk.gormkrings.diff;

import lombok.Data;

@Data
public class RunListItemDto {
    private String id;
    /** ISO-8601 string (avoid requiring Jackson JavaTime module registration). */
    private String createdAt;
    private Long rngSeed;
    private String modelAppVersion;
    private String inputHash;
}
