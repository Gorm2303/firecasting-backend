package dk.gormkrings.statistics.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface YearlySummaryRepository extends JpaRepository<YearlySummaryEntity, String> {
    boolean existsByRunId(String runId);
    long countByRunId(String runId); // optional
    List<YearlySummaryEntity> findByRunIdOrderByPhaseNameAscYearAsc(String runId);
    Optional<YearlySummaryEntity> findByRunIdAndPhaseNameAndYear(String runId, String phaseName, int year);
}
