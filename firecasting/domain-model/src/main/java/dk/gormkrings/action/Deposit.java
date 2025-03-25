package dk.gormkrings.action;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
public class Deposit implements Action {
    @Getter
    private double initial;
    @Getter
    private double monthly;
    private double increaseMonthlyAmount;
    private double increaseMonthlyPercentage;

    public Deposit(double initial, double monthly) {
        this.initial = initial;
        this.monthly = monthly;
        this.increaseMonthlyAmount = 0;
        this.increaseMonthlyPercentage = 0;
        log.debug("Initializing Deposit: {} initial and {} monthly", initial, monthly);
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
        Deposit deposit = new Deposit(
                this.getInitial(),
                this.getMonthly()
        );
        deposit.setIncreaseMonthlyAmount(this.increaseMonthlyAmount);
        deposit.setIncreaseMonthlyPercentage(this.increaseMonthlyPercentage);
        return deposit;
    }
}
