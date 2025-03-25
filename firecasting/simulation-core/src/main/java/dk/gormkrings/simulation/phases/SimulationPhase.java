package dk.gormkrings.simulation.phases;

import dk.gormkrings.data.LiveData;
import dk.gormkrings.event.date.MonthEvent;
import dk.gormkrings.event.date.YearEvent;
import dk.gormkrings.simulation.specification.Specification;
import dk.gormkrings.taxes.NotionalGainsTax;
import dk.gormkrings.util.Util;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

import static dk.gormkrings.util.Util.formatNumber;

@Slf4j
@Getter
@Setter
public abstract class SimulationPhase implements Phase {
    private String name;
    private LocalDate startDate;
    private long duration;
    private Specification specification;

    SimulationPhase(Specification specification, LocalDate startDate, long duration, String name) {
        this.startDate = startDate;
        this.duration = duration;
        this.specification = specification;
        this.name = name;
    }

    @Override
    public void addReturn() {
        double r = specification.getReturner().calculateReturn(getLiveData().getCapital());
        getLiveData().setCurrentReturn(r);
        getLiveData().addToReturned(r);
        getLiveData().addToCapital(r);
    }

    @Override
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

    @Override
    public LiveData getLiveData() {
        return specification.getLiveData();
    }

    @Override
    public LocalDate getCurrentLocalDate() {
        return startDate.plusDays(getLiveData().getSessionDuration() - 1);
    }

    public String getPrettyCurrentDate() {
        LocalDate currentDate = getCurrentLocalDate();
        long days = getLiveData().getSessionDuration();
        int years = (currentDate.getYear()-startDate.getYear());
        int months = currentDate.getMonth().getValue() + years * 12;
        String formattedDate = "";
        formattedDate = (name + " - Day " + days + " - Month " + months + " - Year " + (years+1) + " - " + currentDate);
        return formattedDate;
    }

    public String prettyString() {
        return getPrettyCurrentDate() +
                " - Capital " + formatNumber(getLiveData().getCapital()) +
                " - Deposited " + formatNumber(getLiveData().getDeposited()) +
                " - Returned " + formatNumber(getLiveData().getReturned()) +
                " - Return " + formatNumber(getLiveData().getCurrentReturn()) +
                " - Tax " + Util.formatNumber(getLiveData().getTax()) +
                " - Earnings " + Util.formatNumber(getLiveData().getNetEarnings()
        );
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
