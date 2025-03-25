package dk.gormkrings.action;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
public class Withdraw implements Action {
    private double monthlyAmount;
    private double percent;

    public Withdraw(double monthlyAmount, double percent) {
        this.monthlyAmount = monthlyAmount;
        this.percent = percent;
        log.debug("Initializing withdraw: {} monthly, {} percent", monthlyAmount, percent);
    }

    public double getMonthlyAmount(double capital) {
        if (monthlyAmount > 0) return monthlyAmount;
        else if (percent > 0) return (percent * capital)/12;
        else return 0;
    }

    public double getYearlyAmount(double capital) {
        return getMonthlyAmount(capital)*12;
    }

    public Withdraw copy() {
        return new Withdraw(
                this.monthlyAmount,
                this.percent
        );
    }
}
