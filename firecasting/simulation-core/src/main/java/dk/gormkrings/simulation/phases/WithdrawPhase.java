package dk.gormkrings.simulation.phases;

import dk.gormkrings.action.Withdraw;
import dk.gormkrings.event.Type;
import dk.gormkrings.event.date.MonthEvent;
import dk.gormkrings.event.date.YearEvent;
import dk.gormkrings.simulation.specification.Spec;
import dk.gormkrings.simulation.specification.Specification;
import dk.gormkrings.taxes.CapitalGainsTax;
import dk.gormkrings.taxes.NotionalGainsTax;
import dk.gormkrings.util.Util;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

@Slf4j
@Getter
@Setter
public class WithdrawPhase extends SimulationPhase {
    private Withdraw withdraw;

    public WithdrawPhase(Specification specification, LocalDate startDate, long duration, Withdraw withdraw) {
        super(specification, startDate, duration, "Withdraw");
        log.debug("Initializing Withdraw Phase: {}, for {} days", startDate, duration);
        this.withdraw = withdraw;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        if (event instanceof MonthEvent monthEvent &&
                monthEvent.getType() == Type.END) {
            addReturn();
            withdrawMoney();
            addNetEarnings();
            log.debug(prettyString());

        } else if (event instanceof YearEvent yearEvent &&
                yearEvent.getType() == Type.END) {
            super.addTax();

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
                " - Withdraw " + Util.formatNumber(getLiveData().getWithdraw()) +
                " - Withdrawn " + Util.formatNumber(getLiveData().getWithdrawn());
    }

    @Override
    public WithdrawPhase copy(Spec specificationCopy) {
        return new WithdrawPhase(
                (Specification) specificationCopy,
                getStartDate(),
                getDuration(),
                this.withdraw.copy()
        );
    }
}
