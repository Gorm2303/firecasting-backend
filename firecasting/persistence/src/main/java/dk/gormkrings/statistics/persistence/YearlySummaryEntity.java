package dk.gormkrings.statistics.persistence;

import com.vladmihalcea.hibernate.type.array.DoubleArrayType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(
        name = "yearly_summary",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_summary_run_phase_year", columnNames = {"run_id", "phase_name", "year"})
        },
        indexes = {
                @Index(name = "idx_summary_run", columnList = "run_id"),
                @Index(name = "idx_summary_phase_year", columnList = "phase_name,year")
        }
)
@Getter @Setter @NoArgsConstructor
public class YearlySummaryEntity {

    @Id
    @UuidGenerator
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false, foreignKey = @ForeignKey(name = "fk_summary_run"))
    private SimulationRunEntity run;

    @Column(name = "phase_name", nullable = false)
    private String phaseName;

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "average_capital") private double averageCapital;
    @Column(name = "median_capital")  private double medianCapital;
    @Column(name = "min_capital")     private double minCapital;
    @Column(name = "max_capital")     private double maxCapital;
    @Column(name = "stddev_capital")  private double stdDevCapital;
    @Column(name = "cumulative_growth_rate") private double cumulativeGrowthRate;
    @Column(name = "quantile5")       private double quantile5;
    @Column(name = "quantile25")      private double quantile25;
    @Column(name = "quantile75")      private double quantile75;
    @Column(name = "quantile95")      private double quantile95;
    @Column(name = "var_value")       private double var;
    @Column(name = "cvar_value")      private double cvar;
    @Column(name = "neg_capital_pct") private double negativeCapitalPercentage;

    // 1001 doubles: p0..p100 at 0.1% increments
    @Type(DoubleArrayType.class)
    @Column(name = "percentiles", columnDefinition = "double precision[]", nullable = false)
    private double[] percentiles;
}
