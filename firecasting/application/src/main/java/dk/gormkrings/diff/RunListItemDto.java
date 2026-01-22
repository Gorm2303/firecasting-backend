package dk.gormkrings.diff;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class RunListItemDto {
    private String id;
    private OffsetDateTime createdAt;
    private Long rngSeed;
    private String modelAppVersion;
    private String inputHash;
}
