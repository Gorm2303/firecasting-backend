package dk.gormkrings.phase.callBased;

import dk.gormkrings.phase.IDepositPhase;
import dk.gormkrings.action.Action;
import dk.gormkrings.action.Deposit;
import dk.gormkrings.data.IDate;
import dk.gormkrings.simulation.util.Formatter;
import dk.gormkrings.specification.ISpecification;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class DepositCallPhase extends SimulationCallPhase implements IDepositPhase {
    private Deposit deposit;
    private boolean firstTime = true;

    public DepositCallPhase(ISpecification specification, IDate startDate, long duration, Action deposit) {
        super(specification, startDate, duration, "Deposit");
        log.debug("Initializing Deposit Phase: {}, for {} days", startDate, duration);
        this.deposit = (Deposit) deposit;
    }

    @Override
    public void onMonthEnd() {
        super.onMonthEnd();
        depositMoney();
        if (Formatter.debug) log.debug(prettyString());
    }

    @Override
    public DepositCallPhase copy(ISpecification specificationCopy) {
        return new DepositCallPhase(
                specificationCopy,
                this.getStartDate(),
                getDuration(),
                this.deposit.copy()
        );
    }
}
