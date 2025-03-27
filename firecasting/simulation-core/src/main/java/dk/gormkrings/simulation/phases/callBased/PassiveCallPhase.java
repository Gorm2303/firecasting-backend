package dk.gormkrings.simulation.phases.callBased;

import dk.gormkrings.action.Passive;
import dk.gormkrings.data.IDate;
import dk.gormkrings.simulation.specification.Spec;
import dk.gormkrings.simulation.specification.Specification;

import dk.gormkrings.simulation.util.Formatter;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class PassiveCallPhase extends SimulationCallPhase {
    private Passive passive;
    private boolean firstTime = true;

    public PassiveCallPhase(Specification specification, IDate startDate, long duration, Passive passive) {
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
    public PassiveCallPhase copy(Spec specificationCopy) {
        return new PassiveCallPhase(
                (Specification) specificationCopy,
                getStartDate(),
                getDuration(),
                this.passive.copy()
        );
    }
}
