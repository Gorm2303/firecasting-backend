package dk.gormkrings.statistics.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SimulationRunRepository extends JpaRepository<SimulationRunEntity, String> {
    Optional<SimulationRunEntity> findByInputHash(String inputHash);
}
