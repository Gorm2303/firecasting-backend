package dk.gormkrings.simulation.phases;

import dk.gormkrings.action.Passive;
import dk.gormkrings.data.LiveData;
import dk.gormkrings.event.date.MonthEvent;
import dk.gormkrings.event.Type;
import dk.gormkrings.simulation.specification.Spec;
import dk.gormkrings.simulation.specification.Specification;
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
        log.debug("Initializing Passive Phase");
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
        double passiveReturn = data.getCurrentReturn();
        if (firstTime) {
            passive.setInitial(data.getReturned() - passiveReturn);
            firstTime = false;
        }
        data.setPassiveReturn(passiveReturn);
        data.addToPassiveReturned(passiveReturn);
    }

    @Override
    public String prettyString() {
        return super.prettyString() +
                " - Passive " + formatNumber(getLiveData().getPassiveReturned());
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
