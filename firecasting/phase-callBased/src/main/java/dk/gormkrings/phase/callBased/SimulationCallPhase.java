package dk.gormkrings.phase.callBased;

import dk.gormkrings.phase.ISimulationPhase;
import dk.gormkrings.data.IDate;
import dk.gormkrings.data.ILiveData;
import dk.gormkrings.event.EventType;
import dk.gormkrings.phase.ICallPhase;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.simulation.ReturnStep;
import dk.gormkrings.calendar.TradingCalendar;
import dk.gormkrings.calendar.WeekdayTradingCalendar;
import dk.gormkrings.tax.ITaxExemption;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Getter
@Setter
public abstract class SimulationCallPhase implements ICallPhase, ISimulationPhase {
    private final ReturnStep returnStep;
    private final TradingCalendar tradingCalendar;

    private IDate startDate;
    private long duration;
    private ISpecification specification;
    private List<ITaxExemption> taxExemptions;
    private String name;

    public SimulationCallPhase(ISpecification specification, IDate startDate, List<ITaxExemption> taxExemptions, long duration, String name) {
        this(specification, startDate, taxExemptions, duration, name, ReturnStep.DAILY, new WeekdayTradingCalendar());
    }

    public SimulationCallPhase(
            ISpecification specification,
            IDate startDate,
            List<ITaxExemption> taxExemptions,
            long duration,
            String name,
            ReturnStep returnStep,
            TradingCalendar tradingCalendar
    ) {
        this.startDate = startDate;
        this.duration = duration;
        this.specification = specification;
        this.taxExemptions = taxExemptions;
        this.name = name;

        this.returnStep = (returnStep == null) ? ReturnStep.DAILY : returnStep;
        this.tradingCalendar = (tradingCalendar == null) ? new WeekdayTradingCalendar() : tradingCalendar;
    }

    @Override
    public boolean supportsEvent(EventType eventType) {
        return eventType.equals(EventType.DAY_END)
                || eventType.equals(EventType.MONTH_END)
                || eventType.equals(EventType.YEAR_START)
                || eventType.equals(EventType.YEAR_END);
    }

    @Override
    public void onDayStart() {

    }

    @Override
    public void onDayEnd() {
        if (returnStep == ReturnStep.MONTHLY) return;
        if (getLiveData().getCapital() <= 0) return;

        IDate currentDate = startDate.plusDays(specification.getLiveData().getSessionDuration());
        if (tradingCalendar.isTradingDay(currentDate)) {
            addReturn();
        }
    }

    @Override
    public void onWeekStart() {

    }

    @Override
    public void onWeekEnd() {

    }

    @Override
    public void onMonthStart() {

    }

    @Override
    public void onMonthEnd() {
        if (returnStep == ReturnStep.MONTHLY) {
            if (getLiveData().getCapital() > 0) {
                // Apply the month's return using the regime/distribution for the month that just ended.
                addReturn();
            }
        }

        // Month-end hook for returners (e.g., regime switching) – may be no-op.
        if (specification == null) return;
        if (specification.getReturner() == null) return;
        specification.getReturner().onMonthEnd();
    }

    @Override
    public void onYearStart() {
        for (ITaxExemption rule : getTaxExemptions()) {
            rule.yearlyUpdate();
        }
    }

    @Override
    public void onYearEnd() {
        addNotionalTax();
        compoundInflation();
        applyYearlyFee();
    }

    @Override
    public void onPhaseEnd() {

    }

    @Override
    public void onPhaseStart() {
        getLiveData().setPhaseName(name);
    }

    @Override
    public ILiveData getLiveData() {
        return (ILiveData) specification.getLiveData();
    }

    public String prettyString() {
        return getLiveData().toString();
    }

}
