package dk.gormkrings.statistics.persistence;

import dk.gormkrings.statistics.MetricSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MetricSummaryRepository extends JpaRepository<MetricSummaryEntity, String> {

    boolean existsByRunId(String runId);

    List<MetricSummaryEntity> findByRunIdOrderByScopeAscPhaseNameAscYearAscMetricAsc(String runId);

    List<MetricSummaryEntity> findByRunIdAndScopeOrderByPhaseNameAscYearAscMetricAsc(String runId, MetricSummary.Scope scope);
}
