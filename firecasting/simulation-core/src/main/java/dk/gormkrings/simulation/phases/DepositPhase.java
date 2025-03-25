package dk.gormkrings.simulation.phases;

import dk.gormkrings.action.Deposit;
import dk.gormkrings.event.Type;
import dk.gormkrings.event.date.MonthEvent;
import dk.gormkrings.event.date.YearEvent;
import dk.gormkrings.simulation.specification.Spec;
import dk.gormkrings.simulation.specification.Specification;
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
public class DepositPhase extends SimulationPhase {
    private Deposit deposit;
    private boolean firstTime = true;

    public DepositPhase(Specification specification, LocalDate startDate, long duration, Deposit deposit) {
        super(specification, startDate, duration,"Deposit");
        log.debug("Initializing Deposit Phase: {}, for {} days", startDate, duration);
        this.deposit = deposit;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        if (event instanceof MonthEvent monthEvent &&
                monthEvent.getType() == Type.END) {
            addReturn();
            depositMoney();
            log.debug(prettyString());

        } else if (event instanceof YearEvent yearEvent &&
                yearEvent.getType() == Type.END) {
            addTax();

        }
    }

    public void depositMoney() {
        if (firstTime) {
            getLiveData().addToDeposited(deposit.getInitial());
            getLiveData().addToCapital(deposit.getInitial());
            firstTime = false;
        }
        double depositAmount = deposit.getMonthly();
        getLiveData().setDeposit(depositAmount);
        getLiveData().addToDeposited(depositAmount);
        getLiveData().addToCapital(depositAmount);
        deposit.increaseMonthly(deposit.getMonthlyIncrease());
    }

    @Override
    public String prettyString() {
        return super.prettyString() +
                " - Deposit " + Util.formatNumber(getLiveData().getDeposit()
        );
    }

    @Override
    public DepositPhase copy(Spec specificationCopy) {
        return new DepositPhase(
                (Specification) specificationCopy,
                this.getStartDate(),
                getDuration(),
                this.deposit.copy()
        );
    }
}
