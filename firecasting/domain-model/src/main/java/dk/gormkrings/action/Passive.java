package dk.gormkrings.action;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Passive implements Action {
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
