package dk.gormkrings.simulation.phases.eventBased;

import dk.gormkrings.action.Withdraw;
import dk.gormkrings.data.IDate;
import dk.gormkrings.event.Type;
import dk.gormkrings.event.date.MonthEvent;
import dk.gormkrings.simulation.specification.Spec;
import dk.gormkrings.simulation.specification.Specification;
import dk.gormkrings.simulation.util.Formatter;
import dk.gormkrings.taxes.CapitalGainsTax;
import dk.gormkrings.taxes.NotionalGainsTax;
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

    public WithdrawEventPhase(Specification specification, IDate startDate, long duration, Withdraw withdraw) {
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
            if (Formatter.debug) log.debug(prettyString());

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
            double net = getLiveData().getWithdraw() - getLiveData().getCurrentTax();
            getLiveData().addToNetEarnings(net);
            getLiveData().setCurrentNet(net);
        } else if (getSpecification().getTaxRule() instanceof NotionalGainsTax) {
            double net = getLiveData().getWithdraw();
            getLiveData().addToNetEarnings(net);
            getLiveData().setCurrentNet(net);
        }
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
