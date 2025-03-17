package dk.gormkrings;

import lombok.Getter;
import lombok.Setter;

@Getter
public class Deposit {
    private final double initial;
    @Setter
    private double monthly;
    private double total;

    public Deposit(double initial, double monthly) {
        this.initial = initial;
        this.monthly = monthly;
        this.total = initial;
    }

    public double yearlyIncrement() {
        return total += monthly*12;
    }
}
