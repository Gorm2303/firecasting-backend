package dk.gormkrings.phase.eventBased;

import dk.gormkrings.action.IAction;
import dk.gormkrings.action.IPassive;
import dk.gormkrings.phase.IPassivePhase;
import dk.gormkrings.data.IDate;
import dk.gormkrings.event.Type;
import dk.gormkrings.event.MonthEvent;
import dk.gormkrings.simulation.util.Formatter;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.ITaxRule;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

import java.util.ArrayList;
import java.util.List;

@Getter
@Slf4j
public class PassiveEventPhase extends SimulationEventPhase implements IPassivePhase {
    private IPassive passive;
    @Setter
    private boolean firstTime = true;

    public PassiveEventPhase(ISpecification specification, IDate startDate, List<ITaxRule> taxRules, long duration, IAction passive) {
        super(specification, startDate, taxRules, duration, "Passive");
        log.debug("Initializing Passive Phase: {}, for {} days", startDate, duration);
        this.passive = (IPassive) passive;
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
        List<ITaxRule> taxRulesCopy = new ArrayList<>();
        for (ITaxRule taxRule : getTaxRules()) {
            taxRulesCopy.add(taxRule.copy());
        }
        return new PassiveEventPhase(
                specificationCopy,
                getStartDate(),
                taxRulesCopy,
                getDuration(),
                this.passive.copy()
        );
    }
}
