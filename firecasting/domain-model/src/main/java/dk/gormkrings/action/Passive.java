package dk.gormkrings.action;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Passive implements Action {
    private double initial;

    public Passive() {
        this.initial = 0;
    }

    public void setInitial(double initial) {
        this.initial = initial;
    }

    public double getPassive(double returned) {
        return returned - initial;
    }

    public Passive copy() {
        Passive copy = new Passive();
        copy.setInitial(this.initial);
        return copy;
    }
}
