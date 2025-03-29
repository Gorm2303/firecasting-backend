package dk.gormkrings.phase.eventBased;

import dk.gormkrings.action.Action;
import dk.gormkrings.phase.IWithdrawPhase;
import dk.gormkrings.action.Withdraw;
import dk.gormkrings.data.IDate;
import dk.gormkrings.event.Type;
import dk.gormkrings.event.MonthEvent;
import dk.gormkrings.simulation.util.Formatter;
import dk.gormkrings.specification.ISpecification;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

@Slf4j
@Getter
@Setter
public class WithdrawEventPhase extends SimulationEventPhase implements IWithdrawPhase {
    private Withdraw withdraw;

    public WithdrawEventPhase(ISpecification specification, IDate startDate, long duration, Action withdraw) {
        super(specification, startDate, duration, "Withdraw");
        log.debug("Initializing Withdraw Phase: {}, for {} days", startDate, duration);
        this.withdraw = (Withdraw) withdraw;
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

    @Override
    public WithdrawEventPhase copy(ISpecification specificationCopy) {
        return new WithdrawEventPhase(
                specificationCopy,
                getStartDate(),
                getDuration(),
                this.withdraw.copy()
        );
    }
}
