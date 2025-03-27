package dk.gormkrings.phase.eventBased;

import dk.gormkrings.action.Passive;
import dk.gormkrings.data.IDate;
import dk.gormkrings.event.Type;
import dk.gormkrings.event.MonthEvent;
import dk.gormkrings.simulation.specification.Specification;
import dk.gormkrings.simulation.util.Formatter;
import dk.gormkrings.specification.ISpec;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;


@Slf4j
public class PassiveEventPhase extends SimulationEventPhase {
    private Passive passive;
    private boolean firstTime = true;

    public PassiveEventPhase(ISpec specification, IDate startDate, long duration, Passive passive) {
        super(specification, startDate, duration, "Passive");
        log.debug("Initializing Passive Phase: {}, for {} days", startDate, duration);
        this.passive = passive;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        super.onApplicationEvent(event);
        if (event instanceof MonthEvent monthEvent &&
                monthEvent.getType() == Type.END) {
            calculatePassive();
            if (Formatter.debug) log.debug(prettyString());

        }
    }

    private void calculatePassive() {
        double passiveReturn = 0;
        if (firstTime) {
            firstTime = false;
            passive.setPreviouslyReturned(getLiveData().getReturned());
            passiveReturn = getLiveData().getCurrentReturn();
        } else {
            passiveReturn = getLiveData().getReturned() - passive.getPreviouslyReturned();
            passive.setPreviouslyReturned(getLiveData().getReturned());
        }

        getLiveData().setPassiveReturn(passiveReturn);
        getLiveData().addToPassiveReturned(passiveReturn);
    }

    @Override
    public PassiveEventPhase copy(ISpec specificationCopy) {
        return new PassiveEventPhase(
                (Specification) specificationCopy,
                getStartDate(),
                getDuration(),
                this.passive.copy()
        );
    }
}
