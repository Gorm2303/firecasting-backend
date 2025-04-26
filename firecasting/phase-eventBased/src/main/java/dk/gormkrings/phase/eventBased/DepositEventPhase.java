package dk.gormkrings.phase.eventBased;

import dk.gormkrings.action.IAction;
import dk.gormkrings.action.IDeposit;
import dk.gormkrings.phase.IDepositPhase;
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


@Slf4j
@Getter
@Setter
public class DepositEventPhase extends SimulationEventPhase implements IDepositPhase {
    private IDeposit deposit;
    private boolean firstTime = true;

    public DepositEventPhase(ISpecification specification, IDate startDate, List<ITaxRule> taxRules, long duration, IAction deposit) {
        super(specification, startDate, taxRules, duration, "Deposit");
        log.debug("Initializing Deposit Phase: {}, for {} days", startDate, duration);
        this.deposit = (IDeposit) deposit;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        if (event instanceof MonthEvent monthEvent && monthEvent.getType() == Type.START) {
            depositInitialDeposit();
        }
        super.onApplicationEvent(event);
        if (event instanceof MonthEvent monthEvent && monthEvent.getType() == Type.END) {
            depositMoney();
            if (Formatter.debug) log.debug(prettyString());
        }
    }

    @Override
    public DepositEventPhase copy(ISpecification specificationCopy) {
        List<ITaxRule> taxRulesCopy = new ArrayList<>();
        for (ITaxRule taxRule : getTaxRules()) {
            taxRulesCopy.add(taxRule.copy());
        }
        return new DepositEventPhase(
                specificationCopy,
                this.getStartDate(),
                taxRulesCopy,
                getDuration(),
                this.deposit.copy());
    }
}
