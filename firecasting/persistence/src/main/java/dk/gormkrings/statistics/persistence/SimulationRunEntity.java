package dk.gormkrings.statistics.persistence;

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

    @Lob
    @Column(name = "input_json", nullable = false)
    private String inputJson;

    @Column(name = "input_hash", nullable = false, length = 64)
    private String inputHash;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<YearlySummaryEntity> summaries = new ArrayList<>();
}
