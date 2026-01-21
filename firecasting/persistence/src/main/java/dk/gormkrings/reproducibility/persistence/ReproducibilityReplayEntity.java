package dk.gormkrings.reproducibility.persistence;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "reproducibility_replay")
@Getter
@Setter
@NoArgsConstructor
public class ReproducibilityReplayEntity {

    public enum Status {
        QUEUED,
        RUNNING,
        DONE,
        FAILED
    }

    @Id
    @UuidGenerator
    private String id;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "source_app_version", length = 128)
    private String sourceAppVersion;

    @Column(name = "current_app_version", length = 128)
    private String currentAppVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "bundle_json", columnDefinition = "jsonb", nullable = false)
    private String bundleJson;

    @Column(name = "replay_run_id", length = 36)
    private String replayRunId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "report_json", columnDefinition = "jsonb")
    private String reportJson;
}
