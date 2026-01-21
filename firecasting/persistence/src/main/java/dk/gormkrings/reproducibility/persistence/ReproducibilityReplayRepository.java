package dk.gormkrings.reproducibility.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReproducibilityReplayRepository extends JpaRepository<ReproducibilityReplayEntity, String> {
}
