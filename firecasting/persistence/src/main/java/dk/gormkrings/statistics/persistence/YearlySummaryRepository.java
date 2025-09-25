package dk.gormkrings.statistics.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface YearlySummaryRepository extends JpaRepository<YearlySummaryEntity, String> {

    // Use nested property path
    boolean existsByRunId(String runId);

    // Ditto here; Spring Data resolves 'run.id' via underscore
    List<YearlySummaryEntity> findByRunIdOrderByPhaseNameAscYearAsc(String runId);
}
