package dk.gormkrings.fee;

import org.springframework.stereotype.Component;

@Component
public class DefaultYearlyFeeFactory implements IYearlyFeeFactory {

    @Override
    public IYearlyFee createYearlyFee() {
        return new DefaultYearlyFee(0.0);
    }

    @Override
    public IYearlyFee createYearlyFee(double yearlyFeePercentage) {
        return new DefaultYearlyFee(yearlyFeePercentage);
    }
}
