package dk.gormkrings.simulation.phases;

import dk.gormkrings.action.Action;
import dk.gormkrings.data.LiveData;
import dk.gormkrings.event.date.MonthEvent;
import dk.gormkrings.simulation.specification.Specification;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.text.DecimalFormat;
import java.time.LocalDate;

@Getter
@Setter
public abstract class SimulationPhase implements Phase {
    private String name;
    private LocalDate startDate;
    private long duration;
    private Action action;
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
                " - Return " + formatNumber(getLiveData().getCurrentReturn()
        );
    }

    @Override
    public boolean supportsEventType(@NonNull Class<? extends ApplicationEvent> eventType) {
        return MonthEvent.class.isAssignableFrom(eventType);
    }

    @Override
    public boolean supportsSourceType(Class<?> sourceType) {
        return true;
    }

    public String formatNumber(double number) {
        String pattern;
        if (number > 100) {
            pattern = "0";
        } else if (number <= 100 && number >= 0.1) {
            pattern = "0.00";
        } else {
            pattern = "0.0000";
        }
        DecimalFormat df = new DecimalFormat(pattern);
        return df.format(number);
    }
}
