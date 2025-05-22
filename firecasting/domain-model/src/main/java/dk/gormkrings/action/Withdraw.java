package dk.gormkrings.action;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Withdraw implements IWithdraw {
    private double monthlyAmount;
    @Setter
    private double yearlyPercentage;
    @Setter
    @Getter
    private double dynamicAmountOfReturn;
    @Getter
    private boolean percentageWithdraw = false;
    private final double lowerThreshold;
    private final double upperThreshold;


    public Withdraw(double monthlyAmount, double yearlyPercentage, double lowerThreshold, double upperThreshold) {
        if (monthlyAmount < 0 || yearlyPercentage < 0) throw new IllegalArgumentException("Withdraw constructor called with a negative value");
        this.monthlyAmount = monthlyAmount;
        this.yearlyPercentage = yearlyPercentage;
        this.lowerThreshold = lowerThreshold;
        this.upperThreshold = upperThreshold;
        log.debug("Initializing withdraw: {} monthly, {} percent, {} lower percent, {} upper percent", monthlyAmount, yearlyPercentage, this.lowerThreshold, this.upperThreshold);
    }

    public double getMonthlyAmount(double capital) {
        return getMonthlyAmount(capital, 0);
    }

    @Override
    public double getLowerVariationPercentage() {
        return lowerThreshold;
    }

    @Override
    public double getUpperVariationPercentage() {
        return upperThreshold;
    }

    @Override
    public double getMonthlyAmount(double capital, double inflation) {
        if (monthlyAmount <= 0) {
            monthlyAmount = (yearlyPercentage/100 * capital)/12;
            percentageWithdraw = true;
        }
        return monthlyAmount + (monthlyAmount * (inflation / 100));

    }

    public void setMonthlyAmount(double monthlyAmount) {
        if (monthlyAmount < 0) throw new IllegalArgumentException("MonthlyAmount setter called with a negative value");
        this.monthlyAmount = monthlyAmount;
    }

    @Override
    public Withdraw copy() {
        return new Withdraw(
                this.monthlyAmount,
                this.yearlyPercentage,
                this.lowerThreshold,
                this.upperThreshold
        );
    }
}
