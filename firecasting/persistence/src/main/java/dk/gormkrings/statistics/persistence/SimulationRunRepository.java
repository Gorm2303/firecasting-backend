package dk.gormkrings.statistics.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SimulationRunRepository extends JpaRepository<SimulationRunEntity, String> {
    /**
     * Dedup lookups should be resilient to historical duplicates.
     * Pick the newest run when multiple rows share the same hash.
     */
    Optional<SimulationRunEntity> findFirstByInputHashOrderByCreatedAtDesc(String inputHash);
}
