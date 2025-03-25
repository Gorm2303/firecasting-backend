package dk.gormkrings.action;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class Passive implements Action {
    double previouslyReturned = 0;

    public Passive() {
        log.debug("Initializing Passive");
    }

    public Passive copy() {
        return new Passive();
    }
}
