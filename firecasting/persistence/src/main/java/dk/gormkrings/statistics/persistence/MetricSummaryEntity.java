package dk.gormkrings.statistics.persistence;

import dk.gormkrings.statistics.MetricSummary;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(
    name = "metric_summary",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_metric_summary_run_scope_phase_year_metric",
            columnNames = {"run_id", "scope", "phase_name", "year", "metric"}
        )
    },
    indexes = {
        @Index(name = "idx_metric_summary_run", columnList = "run_id"),
        @Index(name = "idx_metric_summary_scope", columnList = "scope"),
        @Index(name = "idx_metric_summary_phase_year", columnList = "phase_name,year")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class MetricSummaryEntity {

    @Id
    @UuidGenerator
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false, foreignKey = @ForeignKey(name = "fk_metric_summary_run"))
    private SimulationRunEntity run;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false)
    private MetricSummary.Scope scope;

    @Column(name = "phase_name")
    private String phaseName;

    @Column(name = "year")
    private Integer year;

    @Column(name = "metric", nullable = false)
    private String metric;

    @Column(name = "p5")
    private double p5;
    @Column(name = "p10")
    private double p10;
    @Column(name = "p25")
    private double p25;
    @Column(name = "p50")
    private double p50;
    @Column(name = "p75")
    private double p75;
    @Column(name = "p90")
    private double p90;
    @Column(name = "p95")
    private double p95;
}
