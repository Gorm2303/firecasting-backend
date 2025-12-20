package dk.gormkrings.phase.eventBased;

import dk.gormkrings.calendar.TradingCalendar;
import dk.gormkrings.calendar.WeekdayTradingCalendar;
import dk.gormkrings.phase.ISimulationPhase;
import dk.gormkrings.data.IDate;
import dk.gormkrings.data.ILiveData;
import dk.gormkrings.event.Type;
import dk.gormkrings.event.MonthEvent;
import dk.gormkrings.event.YearEvent;
import dk.gormkrings.event.DayEvent;
import dk.gormkrings.phase.IEventPhase;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.simulation.ReturnStep;
import dk.gormkrings.tax.ITaxExemption;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Slf4j
@Getter
@Setter
public abstract class SimulationEventPhase implements IEventPhase, ISimulationPhase {
    private static volatile ReturnStep returnStep = ReturnStep.DAILY;
    private static volatile TradingCalendar tradingCalendar = new WeekdayTradingCalendar();

    private IDate startDate;
    private long duration;
    private ISpecification specification;
    private List<ITaxExemption> taxExemptions;
    private String name;

    public SimulationEventPhase(ISpecification specification, IDate startDate, List<ITaxExemption> taxExemptions, long duration, String name) {
        this.startDate = startDate;
        this.duration = duration;
        this.specification = specification;
        this.taxExemptions = taxExemptions;
        this.name = name;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        if (event instanceof DayEvent) {
            // Treat DayEvent as day-end. Apply returns only on trading days when configured for DAILY.
            if (returnStep == ReturnStep.DAILY && getLiveData().getCapital() > 0) {
                IDate currentDate = startDate.plusDays(specification.getLiveData().getSessionDuration());
                if (tradingCalendar.isTradingDay(currentDate)) {
                    addReturn();
                }
            }
            return;
        }

        if (event instanceof MonthEvent monthEvent && monthEvent.getType() == Type.END) {
            if (returnStep == ReturnStep.MONTHLY && getLiveData().getCapital() > 0) {
                addReturn();
            }

            // Month-end hook for returners (e.g., regime switching)
            if (specification != null && specification.getReturner() != null) {
                specification.getReturner().onMonthEnd();
            }
        } else if (event instanceof YearEvent yearEvent &&
                yearEvent.getType() == Type.END) {
            addNotionalTax();
            compoundInflation();
        }
    }

    public static void configureReturnStep(ReturnStep step) {
        returnStep = (step == null) ? ReturnStep.DAILY : step;
    }

    public static void configureTradingCalendar(TradingCalendar calendar) {
        tradingCalendar = (calendar == null) ? new WeekdayTradingCalendar() : calendar;
    }

    @Override
    public ILiveData getLiveData() {
        return (ILiveData) specification.getLiveData();
    }

    public String prettyString() {
        return name + getLiveData().toString();
    }

    @Override
    public boolean supportsEventType(@NonNull Class<? extends ApplicationEvent> eventType) {
        return DayEvent.class.isAssignableFrom(eventType)
                || MonthEvent.class.isAssignableFrom(eventType)
                || YearEvent.class.isAssignableFrom(eventType);
    }

    @Override
    public boolean supportsSourceType(Class<?> sourceType) {
        return true;
    }
}
