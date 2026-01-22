package dk.gormkrings.fee;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class DefaultYearlyFee implements IYearlyFee {

    /** Percentage per year, e.g. 0.5 == 0.5%. */
    private final double yearlyFeePercentage;

    public DefaultYearlyFee(double yearlyFeePercentage) {
        this.yearlyFeePercentage = yearlyFeePercentage;
        log.debug("DefaultYearlyFee created with yearlyFeePercentage: {}", yearlyFeePercentage);
    }

    @Override
    public double calculateFee(double capital) {
        if (!Double.isFinite(capital) || capital <= 0.0) return 0.0;
        if (!Double.isFinite(yearlyFeePercentage) || yearlyFeePercentage <= 0.0) return 0.0;
        return capital * (yearlyFeePercentage / 100.0);
    }

    @Override
    public IYearlyFee copy() {
        return new DefaultYearlyFee(this.yearlyFeePercentage);
    }
}
