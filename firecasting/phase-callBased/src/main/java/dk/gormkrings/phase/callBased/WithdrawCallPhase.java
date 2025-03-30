package dk.gormkrings.phase.callBased;

import dk.gormkrings.phase.IWithdrawPhase;
import dk.gormkrings.action.IAction;
import dk.gormkrings.action.Withdraw;
import dk.gormkrings.data.IDate;
import dk.gormkrings.simulation.util.Formatter;
import dk.gormkrings.specification.ISpecification;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class WithdrawCallPhase extends SimulationCallPhase implements IWithdrawPhase {
    private Withdraw withdraw;

    public WithdrawCallPhase(ISpecification specification, IDate startDate, long duration, IAction withdraw) {
        super(specification, startDate, duration, "Withdraw");
        log.debug("Initializing Withdraw Phase: {}, for {} days", startDate, duration);
        this.withdraw = (Withdraw) withdraw;
    }

    @Override
    public void onMonthEnd() {
        super.onMonthEnd();
        withdrawMoney();
        addNetEarnings();
        if (Formatter.debug) log.debug(prettyString());
    }

    @Override
    public WithdrawCallPhase copy(ISpecification specificationCopy) {
        return new WithdrawCallPhase(
                specificationCopy,
                getStartDate(),
                getDuration(),
                this.withdraw.copy()
        );
    }
}
