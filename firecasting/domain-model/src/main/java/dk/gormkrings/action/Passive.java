package dk.gormkrings.action;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class Passive implements IPassive {
    private double previouslyReturned = 0;

    public Passive() {
        log.debug("Initializing Passive");
    }

    public Passive copy() {
        Passive copy = new Passive();
        copy.setPreviouslyReturned(this.previouslyReturned);
        return copy;
    }
}
