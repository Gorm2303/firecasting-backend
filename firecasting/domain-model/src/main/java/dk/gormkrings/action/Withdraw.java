package dk.gormkrings.action;

import lombok.Setter;

@Setter
public class Withdraw implements Action {
    private double monthlyAmount;
    private float percent;

    public Withdraw(double monthlyAmount) {
        this.monthlyAmount = monthlyAmount;
        this.percent = 0;
    }

    public Withdraw(float percent) {
        this.percent = percent;
        if (percent >= 1) this.percent %= 100;
        this.monthlyAmount = 0;
    }

    private Withdraw(double monthlyAmount, float percent) {
        this.monthlyAmount = monthlyAmount;
        this.percent = percent;
    }

    public double getMonthlyAmount(double capital) {
        if (monthlyAmount > 0) return monthlyAmount;
        else if (percent > 0) return (percent * capital)/12;
        else return 0;
    }

    public double getYearlyAmount(double capital) {
        if (monthlyAmount > 0) return monthlyAmount*12;
        else if (percent > 0) return percent * capital;
        else return 0;
    }

    public Withdraw copy() {
        return new Withdraw(
                this.monthlyAmount,
                this.percent
        );
    }

}
