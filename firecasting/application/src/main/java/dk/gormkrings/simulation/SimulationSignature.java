package dk.gormkrings.simulation;

/**
 * Canonical signature payload for deduplication and run lookup.
 *
 * Persisted input JSON should remain the original request DTO; this object is only
 * used for hashing (signature) so server-side execution parameters can participate.
 */
public record SimulationSignature(int paths, int batchSize, Object input) {
    public static SimulationSignature of(int paths, int batchSize, Object input) {
        return new SimulationSignature(paths, batchSize, input);
    }
}
