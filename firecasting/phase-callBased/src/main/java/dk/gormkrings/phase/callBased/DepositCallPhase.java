package dk.gormkrings.phase.callBased;

import dk.gormkrings.action.IDeposit;
import dk.gormkrings.event.EventType;
import dk.gormkrings.phase.IDepositPhase;
import dk.gormkrings.action.IAction;
import dk.gormkrings.data.IDate;
import dk.gormkrings.simulation.util.Formatter;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.simulation.ReturnStep;
import dk.gormkrings.calendar.TradingCalendar;
import dk.gormkrings.tax.ITaxExemption;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
@Setter
public class DepositCallPhase extends SimulationCallPhase implements IDepositPhase {
    private IDeposit deposit;

    public DepositCallPhase(ISpecification specification, IDate startDate, List<ITaxExemption> taxExemptions, long duration, IAction deposit) {
        this(specification, startDate, taxExemptions, duration, deposit, ReturnStep.DAILY, null);
    }

    public DepositCallPhase(
            ISpecification specification,
            IDate startDate,
            List<ITaxExemption> taxExemptions,
            long duration,
            IAction deposit,
            ReturnStep returnStep,
            TradingCalendar tradingCalendar
    ) {
        super(specification, startDate, taxExemptions, duration, "Deposit", returnStep, tradingCalendar);
        log.debug("Initializing Deposit Phase: {}, for {} days", startDate, duration);
        this.deposit = (IDeposit) deposit;
    }

    @Override
    public void onMonthEnd() {
        super.onMonthEnd();
        depositMoney();
        if (Formatter.debug) log.debug(prettyString());
    }

    @Override
    public void onYearEnd() {
        super.onYearEnd();
        increaseDeposit();
        log.debug(prettyString());
    }

    @Override
    public void onPhaseStart() {
        super.onPhaseStart();
        depositInitialDeposit();
    }

    @Override
    public boolean supportsEvent(EventType eventType) {
        return super.supportsEvent(eventType) || eventType.equals(EventType.MONTH_END) || eventType.equals(EventType.YEAR_END);
    }

    @Override
    public DepositCallPhase copy(ISpecification specificationCopy) {
        List<ITaxExemption> copy = new ArrayList<>();
        for (ITaxExemption rule : getTaxExemptions()) {
            copy.add(rule.copy());
        }
        return new DepositCallPhase(
                specificationCopy,
                this.getStartDate(),
                copy,
                getDuration(),
                this.deposit.copy(),
                getReturnStep(),
                getTradingCalendar()
        );
    }
}
