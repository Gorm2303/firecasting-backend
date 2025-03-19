package dk.gormkrings;

import jdk.jfr.Percentage;
import lombok.Getter;
import lombok.Setter;

@Setter
public class Withdraw {
    private double amount;
    private float percent;

    public Withdraw(double amount) {
        this.amount = amount;
        this.percent = 0;
    }

    public Withdraw(float percent) {
        this.percent = percent;
        if (percent >= 1) this.percent %= 100;
        this.amount = 0;
    }

    public double getAmount(double capital) {
        if (amount > 0) return amount;
        else if (percent > 0) return percent * capital;
        else return 0;
    }

}
