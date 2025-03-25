package dk.gormkrings.simulation.phases;

import dk.gormkrings.action.Passive;
import dk.gormkrings.event.date.MonthEvent;
import dk.gormkrings.event.Type;
import dk.gormkrings.simulation.specification.Spec;
import dk.gormkrings.simulation.specification.Specification;
import dk.gormkrings.util.Date;
import dk.gormkrings.util.Util;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;


@Slf4j
public class PassivePhase extends SimulationPhase {
    private Passive passive;
    private boolean firstTime = true;

    public PassivePhase(Specification specification, Date startDate, long duration, Passive passive) {
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
            log.debug(prettyString());

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
    public String prettyString() {
        return super.prettyString() +
                getLiveData().getPassiveInfo();
    }

    @Override
    public PassivePhase copy(Spec specificationCopy) {
        return new PassivePhase(
                (Specification) specificationCopy,
                getStartDate(),
                getDuration(),
                this.passive.copy()
        );
    }
}
