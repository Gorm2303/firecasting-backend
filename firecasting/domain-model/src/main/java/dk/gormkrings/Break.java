package dk.gormkrings;

import lombok.Getter;

@Getter
public class Break {
    private double total;
    private double initial;

    public Break() {
        this.initial = 0;
        this.total = initial;
    }

    public void setInitial(double initial) {
        this.initial = initial;
        total = initial;
    }
}
