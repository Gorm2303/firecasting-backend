package dk.gormkrings.simulation.phases.normal;

import dk.gormkrings.action.Deposit;
import dk.gormkrings.simulation.specification.Spec;
import dk.gormkrings.simulation.specification.Specification;
import dk.gormkrings.util.Date;
import dk.gormkrings.util.Util;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class DepositPhase extends SimulationPhase {
    private Deposit deposit;
    private boolean firstTime = true;

    public DepositPhase(Specification specification, Date startDate, long duration, Deposit deposit) {
        super(specification, startDate, duration,"Deposit");
        log.debug("Initializing Deposit Phase: {}, for {} days", startDate, duration);
        this.deposit = deposit;
    }

    @Override
    public void onMonthEnd() {
        super.onMonthEnd();
        depositMoney();
        if (Util.debug) Util.debugLog(prettyString());
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
                getLiveData().getDepositInfo();
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
