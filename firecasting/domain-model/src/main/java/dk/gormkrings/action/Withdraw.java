package dk.gormkrings.action;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Withdraw implements IAction {
    private double monthlyAmount;
    @Setter
    private double monthlyPercent;

    public Withdraw(double monthlyAmount, double monthlyPercent) {
        if (monthlyAmount < 0 || monthlyPercent < 0) throw new IllegalArgumentException("Withdraw constructor called with a negative value");
        this.monthlyAmount = monthlyAmount;
        this.monthlyPercent = monthlyPercent;
        log.debug("Initializing withdraw: {} monthly, {} percent", monthlyAmount, monthlyPercent);
    }

    public double getMonthlyAmount(double capital) {
        if (monthlyAmount > 0) return monthlyAmount;
        else if (monthlyPercent > 0) return (monthlyPercent * capital)/12;
        else return 0;
    }

    public double getMonthlyAmount(double capital, double inflation) {
        if (monthlyAmount > 0) {
            return monthlyAmount * (inflation / 100);
        }
        else if (monthlyPercent > 0) {
            monthlyAmount = (monthlyPercent * capital)/12;
            return (monthlyPercent * capital)/12;
        }
        else return 0;
    }

    public void setMonthlyAmount(double monthlyAmount) {
        if (monthlyAmount < 0) throw new IllegalArgumentException("MonthlyAmount setter called with a negative value");
        this.monthlyAmount = monthlyAmount;
    }

    public Withdraw copy() {
        return new Withdraw(
                this.monthlyAmount,
                this.monthlyPercent
        );
    }
}
