package dk.gormkrings.phase.callBased;

import dk.gormkrings.action.IPassive;
import dk.gormkrings.event.EventType;
import dk.gormkrings.phase.IPassivePhase;
import dk.gormkrings.action.IAction;
import dk.gormkrings.data.IDate;
import dk.gormkrings.simulation.util.Formatter;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.ITaxRule;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Getter
public class PassiveCallPhase extends SimulationCallPhase implements IPassivePhase {
    private IPassive passive;

    public PassiveCallPhase(ISpecification specification, IDate startDate, List<ITaxRule> taxRules, long duration, IAction passive) {
        super(specification, startDate, taxRules, duration, "Passive");
        log.debug("Initializing Passive Phase: {}, for {} days", startDate, duration);
        this.passive = (IPassive) passive;
    }

    @Override
    public void onDayEnd() {
        super.onDayEnd();
        calculatePassive();
    }

    @Override
    public void onMonthEnd() {
        if (Formatter.debug) log.debug(prettyString());
    }

    @Override
    public void onPhaseStart() {
        super.onPhaseStart();
        initializePreviouslyReturned();
    }

    @Override
    public boolean supportsEvent(EventType eventType) {
        return super.supportsEvent(eventType) || eventType.equals(EventType.MONTH_END);
    }

    @Override
    public PassiveCallPhase copy(ISpecification specificationCopy) {
        List<ITaxRule> taxRules = getTaxRules();
        for (ITaxRule rule : getTaxRules()) {
            taxRules.add(rule.copy());
        }
        return new PassiveCallPhase(
                specificationCopy,
                getStartDate(),
                taxRules,
                getDuration(),
                this.passive.copy()
        );
    }
}
