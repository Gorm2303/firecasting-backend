package dk.gormkrings.action;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class Deposit implements IDeposit {
    private final double initial;
    private double monthly;
    private final double yearlyIncreaseInPercent;

    public Deposit(double initial, double monthly, double yearlyIncreaseInPercent) {
        if (initial < 0 || monthly < 0) throw new IllegalArgumentException("Deposit constructor called with a negative initial value");
        this.initial = initial;
        this.monthly = monthly;
        this.yearlyIncreaseInPercent = yearlyIncreaseInPercent;
        log.debug("Initializing Deposit: {} initial and {} monthly", initial, monthly);
    }

    public void setMonthly(double monthly) {
        if (monthly < 0) throw new IllegalArgumentException("Monthly setter called with a negative value");
        this.monthly = monthly;
    }

    public Deposit copy() {
        return new Deposit(
                this.getInitial(),
                this.getMonthly(),
                this.getYearlyIncreaseInPercent()
        );
    }
}
