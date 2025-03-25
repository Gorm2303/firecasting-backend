package dk.gormkrings.simulation.phases;

import dk.gormkrings.action.Passive;
import dk.gormkrings.data.LiveData;
import dk.gormkrings.event.date.MonthEvent;
import dk.gormkrings.event.Type;
import dk.gormkrings.simulation.specification.Spec;
import dk.gormkrings.simulation.specification.Specification;
import dk.gormkrings.util.Util;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

@Slf4j
public class PassivePhase extends SimulationPhase {
    private Passive passive;
    private boolean firstTime = true;

    public PassivePhase(Specification specification, LocalDate startDate, long duration, Passive passive) {
        super(specification, startDate, duration, "Passive");
        log.debug("Initializing Passive Phase: {}, for {} days", startDate, duration);
        this.passive = passive;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        MonthEvent monthEvent = (MonthEvent) event;
        if (monthEvent.getType() != Type.END) return;

        LiveData data = getLiveData();
        addReturn();
        calculatePassive(data);
        log.debug(prettyString());
        }

    private void calculatePassive(LiveData data) {
        double passiveReturn = 0;
        if (firstTime) {
            firstTime = false;
            passive.setPreviouslyReturned(data.getReturned());
            passiveReturn = data.getCurrentReturn();
        } else {
            passiveReturn = data.getReturned() - passive.getPreviouslyReturned();
            passive.setPreviouslyReturned(data.getReturned());
        }

        data.setPassiveReturn(passiveReturn);
        data.addToPassiveReturned(passiveReturn);
    }

    @Override
    public String prettyString() {
        return super.prettyString() +
                " - Passive " + Util.formatNumber(getLiveData().getPassiveReturned());
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
