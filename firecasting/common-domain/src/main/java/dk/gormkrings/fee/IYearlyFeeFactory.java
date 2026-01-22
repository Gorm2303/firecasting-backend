package dk.gormkrings.fee;

public interface IYearlyFeeFactory {

    /**
     * Creates the default yearly fee implementation (typically 0% unless configured otherwise).
     */
    IYearlyFee createYearlyFee();

    /**
     * Creates a fee from a percentage rate (e.g. 0.5 means 0.5% per year).
     */
    IYearlyFee createYearlyFee(double yearlyFeePercentage);
}
