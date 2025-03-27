package dk.gormkrings.phase.eventBased;

import dk.gormkrings.action.Deposit;
import dk.gormkrings.data.IDate;
import dk.gormkrings.event.Type;
import dk.gormkrings.event.MonthEvent;
import dk.gormkrings.simulation.specification.Specification;
import dk.gormkrings.simulation.util.Formatter;
import dk.gormkrings.specification.ISpec;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

@Slf4j
@Getter
@Setter
public class DepositEventPhase extends SimulationEventPhase {
    private Deposit deposit;
    private boolean firstTime = true;

    public DepositEventPhase(ISpec specification, IDate startDate, long duration, Deposit deposit) {
        super(specification, startDate, duration,"Deposit");
        log.debug("Initializing Deposit Phase: {}, for {} days", startDate, duration);
        this.deposit = deposit;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        super.onApplicationEvent(event);
        if (event instanceof MonthEvent monthEvent &&
                monthEvent.getType() == Type.END) {
            depositMoney();
            if (Formatter.debug) log.debug(prettyString());

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
    public DepositEventPhase copy(ISpec specificationCopy) {
        return new DepositEventPhase(
                (Specification) specificationCopy,
                this.getStartDate(),
                getDuration(),
                this.deposit.copy()
        );
    }
}
