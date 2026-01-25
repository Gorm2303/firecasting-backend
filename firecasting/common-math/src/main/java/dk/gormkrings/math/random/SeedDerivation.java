package dk.gormkrings.math.random;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hash-based deterministic seed derivation for independent RNG streams.
 *
 * <p>Goal: expose a single master seed (user-visible), while deriving stable sub-seeds
 * for different stochastic modules (e.g. returns vs inflation) so refactors don't
 * accidentally couple streams.</p>
 */
public final class SeedDerivation {

    private static final String VERSION = "v1";

    private SeedDerivation() {
    }

    public static long derive64(long masterSeed, String label) {
        return derive64(masterSeed, label, null);
    }

    public static long derive64(long masterSeed, String label, Long pathIndex) {
        String l = (label == null) ? "" : label;
        String payload = (pathIndex == null)
                ? masterSeed + "|" + l + "|" + VERSION
                : masterSeed + "|" + l + "|" + pathIndex + "|" + VERSION;

        byte[] digest = sha256(payload);

        // Take first 8 bytes as signed long, big-endian.
        return ByteBuffer.wrap(digest, 0, 8).order(ByteOrder.BIG_ENDIAN).getLong();
    }

    private static byte[] sha256(String payload) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(payload.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
