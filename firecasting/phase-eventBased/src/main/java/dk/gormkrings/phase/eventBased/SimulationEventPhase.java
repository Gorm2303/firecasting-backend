package dk.gormkrings.phase.eventBased;

import dk.gormkrings.data.IDate;
import dk.gormkrings.data.ILiveData;
import dk.gormkrings.event.Type;
import dk.gormkrings.event.MonthEvent;
import dk.gormkrings.event.YearEvent;
import dk.gormkrings.inflation.Inflation;
import dk.gormkrings.phase.IEventPhase;
import dk.gormkrings.simulation.util.Formatter;
import dk.gormkrings.specification.ISpec;
import dk.gormkrings.tax.NotionalGainsTax;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

@Slf4j
@Getter
@Setter
public abstract class SimulationEventPhase implements IEventPhase {
    private IDate startDate;
    private long duration;
    private ISpec specification;
    private String name;

    SimulationEventPhase(ISpec specification, IDate startDate, long duration, String name) {
        this.startDate = startDate;
        this.duration = duration;
        this.specification = specification;
        this.name = name;
    }

    public void addReturn() {
        double r = specification.getReturner().calculateReturn(getLiveData().getCapital());
        getLiveData().setCurrentReturn(r);
        getLiveData().addToReturned(r);
        getLiveData().addToCapital(r);
    }

    public void addTax() {
        if (specification.getTaxRule() instanceof NotionalGainsTax notionalTax) {
            double tax = notionalTax.calculateTax(getLiveData().getReturned() - notionalTax.getPreviousReturned());
            getLiveData().subtractFromCapital(tax);
            getLiveData().subtractFromReturned(tax);
            getLiveData().setCurrentTax(tax);
            getLiveData().addToTax(tax);
            notionalTax.setPreviousReturned(getLiveData().getReturned());

            log.debug("Year {}: NotionalGainsTax calculating tax: {}",
                    getLiveData().getSessionDuration() / 365,
                    Formatter.formatNumber(getLiveData().getCurrentTax()));
        }
    }

    public void addInflation() {
        Inflation inflation = getSpecification().getInflation();
        double inflationAmount = inflation.calculatePercentage();
        getLiveData().addToInflation(inflationAmount);

        log.debug("Year {}: DataAverageInflation calculating inflation: {}",
                getLiveData().getSessionDuration() / 365,
                Formatter.formatNumber(inflationAmount));

    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        if (event instanceof MonthEvent monthEvent &&
                monthEvent.getType() == Type.END) {
            addReturn();
        } else if (event instanceof YearEvent yearEvent &&
                yearEvent.getType() == Type.END) {
            addTax();
            addInflation();

        }
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
        return MonthEvent.class.isAssignableFrom(eventType) || YearEvent.class.isAssignableFrom(eventType);
    }

    @Override
    public boolean supportsSourceType(Class<?> sourceType) {
        return true;
    }
}
