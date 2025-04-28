package dk.gormkrings.phase.callBased;

import dk.gormkrings.action.IWithdraw;
import dk.gormkrings.event.EventType;
import dk.gormkrings.phase.IWithdrawPhase;
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
public class WithdrawCallPhase extends SimulationCallPhase implements IWithdrawPhase {
    private IWithdraw withdraw;
    private double totalReturnLastMonth;

    public WithdrawCallPhase(ISpecification specification, IDate startDate, List<ITaxRule> taxRules, long duration, IAction withdraw) {
        super(specification, startDate, taxRules, duration, "Withdraw");
        log.debug("Initializing Withdraw Phase: {}, for {} days", startDate, duration);
        this.withdraw = (IWithdraw) withdraw;
    }

    @Override
    public void onMonthEnd() {
        if (getLiveData().getCapital() <= 0.0001) return;
        super.onMonthEnd();
        setDynamicWithdraw();
        withdrawMoney();
        addCapitalTax();
        addNetEarnings();
        if (Formatter.debug) log.debug(prettyString());
    }

    private void setDynamicWithdraw() {
        double returnThisMonth = getLiveData().getReturned() - totalReturnLastMonth;
        double monthlyAmount = withdraw.getMonthlyAmount(getLiveData().getCapital(), getLiveData().getInflation());
        if (monthlyAmount < 0.0) return;
        double proportion = returnThisMonth / monthlyAmount;

        if (proportion > 1 + getWithdraw().getUpperVariationPercentage() / 100) {
            withdraw.setDynamicAmountOfReturn((monthlyAmount) * withdraw.getUpperVariationPercentage() / 100);
        } else if (proportion < 1 - withdraw.getLowerVariationPercentage() / 100) {
            withdraw.setDynamicAmountOfReturn((monthlyAmount) * -withdraw.getLowerVariationPercentage() / 100);
        } else {
            withdraw.setDynamicAmountOfReturn((monthlyAmount) * proportion / 100);
        }
        totalReturnLastMonth = getLiveData().getReturned();
    }

    @Override
    public void onPhaseStart() {
        super.onPhaseStart();
        totalReturnLastMonth = getLiveData().getReturned();
    }

    @Override
    public boolean supportsEvent(EventType eventType) {
        return eventType.equals(EventType.MONTH_END) || super.supportsEvent(eventType) || eventType.equals(EventType.PHASE_START);
    }

    @Override
    public WithdrawCallPhase copy(ISpecification specificationCopy) {
        List<ITaxRule> copy = new ArrayList<>();
        for (ITaxRule rule : getTaxRules()) {
            copy.add(rule.copy());
        }
        return new WithdrawCallPhase(
                specificationCopy,
                getStartDate(),
                copy,
                getDuration(),
                this.withdraw.copy());
    }
}
