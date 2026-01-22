package dk.gormkrings.fee;

/**
 * Yearly fee applied to portfolio capital (e.g. management fee).
 *
 * Implementations should return an absolute fee amount to deduct from capital.
 */
public interface IYearlyFee {

    /**
     * Calculates the yearly fee amount (absolute currency) to deduct from the given capital.
     */
    double calculateFee(double capital);

    IYearlyFee copy();
}
