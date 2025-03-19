package dk.gormkrings;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Deposit {
    private double initial;
    private double monthly;
    private double total;
    private double increaseMonthlyAmount;
    private double increaseMonthlyRate;

    public Deposit() {
        this.initial = 0;
        this.monthly = 0;
        this.total = initial;
        this.increaseMonthlyAmount = 0;
        this.increaseMonthlyRate = 0;
    }

    public Deposit(double initial, double monthly) {
        this.initial = initial;
        this.monthly = monthly;
        this.total = initial;
        this.increaseMonthlyAmount = 0;
        this.increaseMonthlyRate = 0;
    }

    public void addMonthly() {
        total += monthly;
    }

    public void increaseMonthly() {
       if (increaseMonthlyAmount != 0) {
           monthly += increaseMonthlyAmount;
       } else if (increaseMonthlyRate != 0) {
           monthly += monthly * increaseMonthlyRate;
       }
    }

    public void setInitial(double initial) {
        this.initial = initial;
        total = initial;
    }
}
