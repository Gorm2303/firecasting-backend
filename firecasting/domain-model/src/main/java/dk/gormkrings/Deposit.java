package dk.gormkrings;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Deposit {
    private double initial;
    private double monthly;
    private double increaseMonthlyAmount;
    private double increaseMonthlyPercentage;

    public Deposit() {
        this.initial = 0;
        this.monthly = 0;
        this.increaseMonthlyAmount = 0;
        this.increaseMonthlyPercentage = 0;
    }

    public Deposit(double initial, double monthly) {
        this.initial = initial;
        this.monthly = monthly;
        this.increaseMonthlyAmount = 0;
        this.increaseMonthlyPercentage = 0;
    }

    public void increaseMonthly() {
       if (increaseMonthlyAmount != 0) {
           monthly += increaseMonthlyAmount;
       } else if (increaseMonthlyPercentage != 0) {
           monthly += monthly * increaseMonthlyPercentage;
       }
    }
}
