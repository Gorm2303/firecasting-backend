package dk.gormkrings.statistics.persistence;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "simulation_run",
        indexes = {
                @Index(name = "idx_simrun_input_hash", columnList = "input_hash")
        }
)
@Getter @Setter @NoArgsConstructor
public class SimulationRunEntity {

    // You provide this as a String UUID (no generator)
    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private String id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_json", columnDefinition = "jsonb", nullable = false)
    private String inputJson; // (or JsonNode if you prefer)

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resolved_input_json", columnDefinition = "jsonb")
    private String resolvedInputJson; // Resolved AdvancedSimulationRequest with all defaults applied

    @Column(name = "input_hash", nullable = false, length = 64)
    private String inputHash;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

        // --- Metadata captured at run creation time (used for diff/attribution) ---

        @Column(name = "model_app_version", length = 128)
        private String modelAppVersion;

        @Column(name = "model_build_time", length = 64)
        private String modelBuildTime;

        @Column(name = "model_spring_boot_version", length = 64)
        private String modelSpringBootVersion;

        @Column(name = "model_java_version", length = 64)
        private String modelJavaVersion;

        @Column(name = "rng_seed")
        private Long rngSeed;

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<YearlySummaryEntity> summaries = new ArrayList<>();
}
