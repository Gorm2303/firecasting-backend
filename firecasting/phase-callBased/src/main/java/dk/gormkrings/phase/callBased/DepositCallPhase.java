package dk.gormkrings.phase.callBased;

import dk.gormkrings.action.IDeposit;
import dk.gormkrings.event.EventType;
import dk.gormkrings.phase.IDepositPhase;
import dk.gormkrings.action.IAction;
import dk.gormkrings.data.IDate;
import dk.gormkrings.simulation.util.Formatter;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.ITaxRule;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
@Setter
public class DepositCallPhase extends SimulationCallPhase implements IDepositPhase {
    private IDeposit deposit;

    public DepositCallPhase(ISpecification specification, IDate startDate, List<ITaxRule> taxRules, long duration, IAction deposit) {
        super(specification, startDate, taxRules, duration, "Deposit");
        log.debug("Initializing Deposit Phase: {}, for {} days", startDate, duration);
        this.deposit = (IDeposit) deposit;
    }

    @Override
    public void onMonthEnd() {
        super.onMonthEnd();
        depositMoney();
        if (Formatter.debug) log.debug(prettyString());
    }

    @Override
    public void onYearEnd() {
        super.onYearEnd();
        increaseDeposit();
        log.debug(prettyString());
    }

    @Override
    public void onPhaseStart() {
        super.onPhaseStart();
        depositInitialDeposit();
    }

    @Override
    public boolean supportsEvent(EventType eventType) {
        return super.supportsEvent(eventType) || eventType.equals(EventType.MONTH_END) || eventType.equals(EventType.YEAR_END);
    }

    @Override
    public DepositCallPhase copy(ISpecification specificationCopy) {
        List<ITaxRule> taxRulesCopy = new ArrayList<>();
        for (ITaxRule taxRule : getTaxRules()) {
            taxRulesCopy.add(taxRule.copy());
        }
        return new DepositCallPhase(
                specificationCopy,
                this.getStartDate(),
                taxRulesCopy,
                getDuration(),
                this.deposit.copy()
        );
    }
}
