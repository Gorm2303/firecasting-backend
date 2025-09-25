package dk.gormkrings.statistics.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface YearlySummaryRepository extends JpaRepository<YearlySummaryEntity, Long> {

    boolean existsByRunId(String runId);

    List<YearlySummaryEntity> findByRunIdOrderByPhaseNameAscYearAsc(String runId);

    @Modifying
    @Transactional
    @Query("DELETE FROM YearlySummaryEntity y WHERE y.run.id = :runId")
    void deleteByRunId(String runId);
}

