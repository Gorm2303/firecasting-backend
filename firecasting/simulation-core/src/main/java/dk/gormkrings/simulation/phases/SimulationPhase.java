package dk.gormkrings.simulation.phases;

import dk.gormkrings.data.LiveData;
import dk.gormkrings.event.Type;
import dk.gormkrings.event.date.MonthEvent;
import dk.gormkrings.event.date.YearEvent;
import dk.gormkrings.inflation.Inflation;
import dk.gormkrings.simulation.specification.Specification;
import dk.gormkrings.taxes.NotionalGainsTax;
import dk.gormkrings.util.Date;
import dk.gormkrings.util.Util;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

@Slf4j
@Getter
@Setter
public abstract class SimulationPhase implements Phase {
    private Date startDate;
    private long duration;
    private Specification specification;
    private String name;

    SimulationPhase(Specification specification, Date startDate, long duration, String name) {
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
                    Util.formatNumber(getLiveData().getCurrentTax()));
        }
    }

    public void addInflation() {
        Inflation inflation = getSpecification().getInflation();
        double inflationAmount = inflation.calculatePercentage();
        getLiveData().addToInflation(inflationAmount);

        log.debug("Year {}: DataAverageInflation calculating inflation: {}",
                getLiveData().getSessionDuration() / 365,
                Util.formatNumber(inflationAmount));

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
    public LiveData getLiveData() {
        return specification.getLiveData();
    }

    public String getPrettyCurrentDate() {
        return Util.getPrettyDate(startDate, getLiveData());
    }

    public String prettyString() {
        return name + getPrettyCurrentDate() +
                getLiveData().getCapitalInfo() +
                getLiveData().getDepositedInfo() +
                getLiveData().getReturnedInfo() +
                getLiveData().getReturnInfo() +
                getLiveData().getTaxedInfo() +
                getLiveData().getEarningsInfo();
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
