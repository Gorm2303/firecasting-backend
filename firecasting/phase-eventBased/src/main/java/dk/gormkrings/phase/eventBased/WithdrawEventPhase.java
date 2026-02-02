package dk.gormkrings.phase.eventBased;

import dk.gormkrings.action.IAction;
import dk.gormkrings.action.IWithdraw;
import dk.gormkrings.phase.IWithdrawPhase;
import dk.gormkrings.data.IDate;
import dk.gormkrings.event.Type;
import dk.gormkrings.event.MonthEvent;
import dk.gormkrings.event.RunEvent;
import dk.gormkrings.simulation.util.Formatter;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.simulation.ReturnStep;
import dk.gormkrings.calendar.TradingCalendar;
import dk.gormkrings.tax.ITaxExemption;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
@Setter
public class WithdrawEventPhase extends SimulationEventPhase implements IWithdrawPhase {
    private IWithdraw withdraw;

    public WithdrawEventPhase(ISpecification specification, IDate startDate, List<ITaxExemption> taxExemptions, long duration, IAction withdraw) {
        this(specification, startDate, taxExemptions, duration, withdraw, ReturnStep.DAILY, null);
    }

    public WithdrawEventPhase(
            ISpecification specification,
            IDate startDate,
            List<ITaxExemption> taxExemptions,
            long duration,
            IAction withdraw,
            ReturnStep returnStep,
            TradingCalendar tradingCalendar
    ) {
        super(specification, startDate, taxExemptions, duration, "Withdraw", returnStep, tradingCalendar);
        log.debug("Initializing Withdraw Phase: {}, for {} days", startDate, duration);
        this.withdraw = (IWithdraw) withdraw;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        super.onApplicationEvent(event);

        // If the phase duration is 0 days (0 months), allow a one-shot withdrawal at phase start.
        // EventEngine emits RunEvent START/END around each phase.
        if (event instanceof RunEvent runEvent && runEvent.getType() == Type.START) {
            if (getDuration() <= 0 && getLiveData().getCapital() > 0.0001) {
                withdrawMoney();
                addCapitalTax();
                addNetEarnings();
                if (Formatter.debug) log.debug(prettyString());
            }
            return;
        }

        if (event instanceof MonthEvent monthEvent &&
                monthEvent.getType() == Type.END) {
            withdrawMoney();
            addCapitalTax();
            addNetEarnings();
            if (Formatter.debug) log.debug(prettyString());
        }
    }

    @Override
    public WithdrawEventPhase copy(ISpecification specificationCopy) {
        List<ITaxExemption> copy = new ArrayList<>();
        for (ITaxExemption rule : getTaxExemptions()) {
            copy.add(rule.copy());
        }
        return new WithdrawEventPhase(
                specificationCopy,
                getStartDate(),
                copy,
                getDuration(),
                this.withdraw.copy(),
                getReturnStep(),
                getTradingCalendar()
        );
    }
}
