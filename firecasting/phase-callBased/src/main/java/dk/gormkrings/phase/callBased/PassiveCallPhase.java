package dk.gormkrings.phase.callBased;

import dk.gormkrings.phase.IPassivePhase;
import dk.gormkrings.action.IAction;
import dk.gormkrings.action.Passive;
import dk.gormkrings.data.IDate;
import dk.gormkrings.simulation.util.Formatter;
import dk.gormkrings.specification.ISpecification;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class PassiveCallPhase extends SimulationCallPhase implements IPassivePhase {
    private Passive passive;

    public PassiveCallPhase(ISpecification specification, IDate startDate, long duration, IAction passive) {
        super(specification, startDate, duration, "Passive");
        log.debug("Initializing Passive Phase: {}, for {} days", startDate, duration);
        this.passive = (Passive) passive;
    }

    @Override
    public void onMonthEnd() {
        super.onMonthEnd();
        calculatePassive();
        if (Formatter.debug) log.debug(prettyString());
    }

    @Override
    public void onPhaseStart() {
        initializePreviouslyReturned();
    }

    @Override
    public PassiveCallPhase copy(ISpecification specificationCopy) {
        return new PassiveCallPhase(
                specificationCopy,
                getStartDate(),
                getDuration(),
                this.passive.copy()
        );
    }
}
