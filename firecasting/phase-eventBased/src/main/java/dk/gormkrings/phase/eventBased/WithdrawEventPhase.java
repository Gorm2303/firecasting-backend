package dk.gormkrings.phase.eventBased;

import dk.gormkrings.action.IAction;
import dk.gormkrings.action.IWithdraw;
import dk.gormkrings.phase.IWithdrawPhase;
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

import java.util.List;

@Slf4j
@Getter
@Setter
public class WithdrawEventPhase extends SimulationEventPhase implements IWithdrawPhase {
    private IWithdraw withdraw;

    public WithdrawEventPhase(ISpecification specification, IDate startDate, List<ITaxRule> taxRules, long duration, IAction withdraw) {
        super(specification, startDate, taxRules, duration, "Withdraw");
        log.debug("Initializing Withdraw Phase: {}, for {} days", startDate, duration);
        this.withdraw = (IWithdraw) withdraw;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        super.onApplicationEvent(event);
        if (event instanceof MonthEvent monthEvent &&
                monthEvent.getType() == Type.END) {
            withdrawMoney();
            addTax();
            addNetEarnings();
            if (Formatter.debug) log.debug(prettyString());
        }
    }

    @Override
    public WithdrawEventPhase copy(ISpecification specificationCopy) {
        List<ITaxRule> taxRules = getTaxRules();
        for (ITaxRule rule : getTaxRules()) {
            taxRules.add(rule.copy());
        }
        return new WithdrawEventPhase(
                specificationCopy,
                getStartDate(),
                taxRules,
                getDuration(),
                this.withdraw.copy()
        );
    }
}
