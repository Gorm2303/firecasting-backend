package dk.gormkrings.phase.callBased;

import dk.gormkrings.action.Passive;
import dk.gormkrings.data.IDate;
import dk.gormkrings.simulation.specification.Specification;
import dk.gormkrings.simulation.util.Formatter;
import dk.gormkrings.specification.ISpec;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class PassiveCallPhase extends SimulationCallPhase {
    private Passive passive;
    private boolean firstTime = true;

    public PassiveCallPhase(ISpec specification, IDate startDate, long duration, Passive passive) {
        super(specification, startDate, duration, "Passive");
        log.debug("Initializing Passive Phase: {}, for {} days", startDate, duration);
        this.passive = passive;
    }

    @Override
    public void onMonthEnd() {
        super.onMonthEnd();
        calculatePassive();
        if (Formatter.debug) log.debug(prettyString());
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
    public PassiveCallPhase copy(ISpec specificationCopy) {
        return new PassiveCallPhase(
                (Specification) specificationCopy,
                getStartDate(),
                getDuration(),
                this.passive.copy()
        );
    }
}
