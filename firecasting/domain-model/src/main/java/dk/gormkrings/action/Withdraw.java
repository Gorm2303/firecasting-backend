package dk.gormkrings.action;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Withdraw implements IWithdraw {
    private double monthlyAmount;
    @Setter
    private double monthlyPercent;
    @Setter
    private double dynamicAmountOfReturn;
    @Getter
    private final double dynamicPercentOfReturn;

    public Withdraw(double monthlyAmount, double monthlyPercent, double dynamicPercentOfReturn) {
        if (monthlyAmount < 0 || monthlyPercent < 0) throw new IllegalArgumentException("Withdraw constructor called with a negative value");
        this.monthlyAmount = monthlyAmount;
        this.monthlyPercent = monthlyPercent;
        this.dynamicPercentOfReturn = dynamicPercentOfReturn;
        log.debug("Initializing withdraw: {} monthly, {} percent, {} dynamic percent", monthlyAmount, monthlyPercent, dynamicPercentOfReturn);
    }

    public double getMonthlyAmount(double capital) {
        return getMonthlyAmount(capital, 0);
    }

    public double getMonthlyAmount(double capital, double inflation) {
        if (monthlyAmount > 0) {
            return monthlyAmount + (monthlyAmount * (inflation / 100)) + dynamicAmountOfReturn;
        } else {
            monthlyAmount = (monthlyPercent * capital)/12;
            return getMonthlyAmount(capital, inflation);
        }
    }

    public void setMonthlyAmount(double monthlyAmount) {
        if (monthlyAmount < 0) throw new IllegalArgumentException("MonthlyAmount setter called with a negative value");
        this.monthlyAmount = monthlyAmount;
    }

    public Withdraw copy() {
        return new Withdraw(
                this.monthlyAmount,
                this.monthlyPercent,
                this.dynamicPercentOfReturn
        );
    }
}
