package dk.gormkrings.phase.eventBased;

import dk.gormkrings.action.IAction;
import dk.gormkrings.phase.IPassivePhase;
import dk.gormkrings.action.Passive;
import dk.gormkrings.data.IDate;
import dk.gormkrings.event.Type;
import dk.gormkrings.event.MonthEvent;
import dk.gormkrings.simulation.util.Formatter;
import dk.gormkrings.specification.ISpecification;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

@Getter
@Slf4j
public class PassiveEventPhase extends SimulationEventPhase implements IPassivePhase {
    private Passive passive;
    @Setter
    private boolean firstTime = true;

    public PassiveEventPhase(ISpecification specification, IDate startDate, long duration, IAction passive) {
        super(specification, startDate, duration, "Passive");
        log.debug("Initializing Passive Phase: {}, for {} days", startDate, duration);
        this.passive = (Passive) passive;
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

    @Override
    public PassiveEventPhase copy(ISpecification specificationCopy) {
        return new PassiveEventPhase(
                specificationCopy,
                getStartDate(),
                getDuration(),
                this.passive.copy()
        );
    }
}
