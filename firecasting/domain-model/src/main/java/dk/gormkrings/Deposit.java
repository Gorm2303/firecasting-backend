package dk.gormkrings;

import lombok.Getter;

@Getter
public class Deposit {
    private double initial;
    private double monthly;
    private double total;

    public Deposit(double initial, double monthly) {
        this.initial = initial;
        this.monthly = monthly;
        this.total = initial;
    }

    public void addMonthly() {
        total += monthly;
    }

    public void increaseMonthly(int amount) {
        monthly += amount;
    }

    public void setInitial(double initial) {
        this.initial = initial;
        total = initial;
    }
}
