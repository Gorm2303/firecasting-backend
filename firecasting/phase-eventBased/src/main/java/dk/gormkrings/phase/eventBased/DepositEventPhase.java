package dk.gormkrings.phase.eventBased;

import dk.gormkrings.phase.IDepositPhase;
import dk.gormkrings.action.Deposit;
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
public class DepositEventPhase extends SimulationEventPhase implements IDepositPhase {
    private Deposit deposit;
    private boolean firstTime = true;

    public DepositEventPhase(ISpecification specification, IDate startDate, long duration, Deposit deposit) {
        super(specification, startDate, duration, "Deposit");
        log.debug("Initializing Deposit Phase: {}, for {} days", startDate, duration);
        this.deposit = deposit;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        if (event instanceof MonthEvent monthEvent && monthEvent.getType() == Type.START) {
            depositInitialDeposit();
        }
        super.onApplicationEvent(event);
        if (event instanceof MonthEvent monthEvent && monthEvent.getType() == Type.END) {
            depositMoney();
            if (Formatter.debug) log.debug(prettyString());
        }
    }

    @Override
    public DepositEventPhase copy(ISpecification specificationCopy) {
        return new DepositEventPhase(
                specificationCopy,
                this.getStartDate(),
                getDuration(),
                this.deposit.copy()
        );
    }
}
