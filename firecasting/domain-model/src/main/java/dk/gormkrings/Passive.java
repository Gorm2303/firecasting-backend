package dk.gormkrings;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Passive {
    private double total;
    private double initial;

    public Passive() {
        this.initial = 0;
        this.total = initial;
    }

    public void setInitial(double initial) {
        this.initial = initial;
        total = initial;
    }
}
