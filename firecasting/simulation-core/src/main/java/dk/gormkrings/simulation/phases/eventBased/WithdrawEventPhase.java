package dk.gormkrings.simulation.phases.eventBased;

import dk.gormkrings.action.Withdraw;
import dk.gormkrings.event.Type;
import dk.gormkrings.event.date.MonthEvent;
import dk.gormkrings.simulation.specification.Spec;
import dk.gormkrings.simulation.specification.Specification;
import dk.gormkrings.taxes.CapitalGainsTax;
import dk.gormkrings.taxes.NotionalGainsTax;
import dk.gormkrings.util.Date;
import dk.gormkrings.util.Util;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;


@Slf4j
@Getter
@Setter
public class WithdrawEventPhase extends SimulationEventPhase {
    private Withdraw withdraw;

    public WithdrawEventPhase(Specification specification, Date startDate, long duration, Withdraw withdraw) {
        super(specification, startDate, duration, "Withdraw");
        log.debug("Initializing Withdraw Phase: {}, for {} days", startDate, duration);
        this.withdraw = withdraw;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        super.onApplicationEvent(event);
        if (event instanceof MonthEvent monthEvent &&
                monthEvent.getType() == Type.END) {
            withdrawMoney();
            addNetEarnings();
            if (Util.debug) Util.debugLog(prettyString());

        }
    }

    public void withdrawMoney() {
        double withdrawAmount = this.withdraw.getMonthlyAmount(getLiveData().getCapital());
        getLiveData().setWithdraw(withdrawAmount);
        getLiveData().addToWithdrawn(withdrawAmount);
        getLiveData().subtractFromCapital(withdrawAmount);
        addTax();
    }

    @Override
    public void addTax() {
        if (getSpecification().getTaxRule() instanceof CapitalGainsTax capitalTax) {
            double tax = capitalTax.calculateTax(getLiveData().getWithdraw());
            getLiveData().setCurrentTax(tax);
            getLiveData().addToTax(tax);
        }
    }

    public void addNetEarnings() {
        if (getSpecification().getTaxRule() instanceof CapitalGainsTax) {
            getLiveData().addToNetEarnings(getLiveData().getWithdraw() - getLiveData().getCurrentTax());
        } else if (getSpecification().getTaxRule() instanceof NotionalGainsTax) {
            getLiveData().addToNetEarnings(getLiveData().getWithdraw());
        }
    }

    @Override
    public String prettyString() {
        return super.prettyString() +
                getLiveData().getWithdrawInfo() +
                getLiveData().getWithdrawnInfo();
    }

    @Override
    public WithdrawEventPhase copy(Spec specificationCopy) {
        return new WithdrawEventPhase(
                (Specification) specificationCopy,
                getStartDate(),
                getDuration(),
                this.withdraw.copy()
        );
    }
}
