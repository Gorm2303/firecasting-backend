package dk.gormkrings;

import lombok.Getter;
import lombok.Setter;

@Setter
public class Deposit {
    @Getter
    private double initial;
    @Getter
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

    public double getMonthlyIncrease() {
       if (increaseMonthlyAmount > 0)
           return increaseMonthlyAmount;
       else if (increaseMonthlyPercentage > 0)
           return monthly * increaseMonthlyPercentage;
       else return 0;
    }

    public void increaseMonthly(double amount) {
        increaseMonthlyAmount += amount;
    }

    public Deposit copy() {
        Deposit deposit = new Deposit();
        deposit.setMonthly(this.getMonthly());
        deposit.setInitial(this.getInitial());
        deposit.setIncreaseMonthlyAmount(this.increaseMonthlyAmount);
        deposit.setIncreaseMonthlyPercentage(this.increaseMonthlyPercentage);
        return deposit;
    }
}
