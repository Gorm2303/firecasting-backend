package dk.gormkrings.simulation.phases;

import dk.gormkrings.data.LiveData;
import dk.gormkrings.event.date.MonthEvent;
import dk.gormkrings.taxes.TaxRule;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;

import java.text.DecimalFormat;
import java.time.LocalDate;

@Getter
@Setter
public abstract class SimulationPhase implements Phase, SmartApplicationListener {
    private LiveData liveData;
    private String name;
    private LocalDate startDate;
    private long duration;
    private TaxRule taxRule;

    @Override
    public void incrementTime() {
        if (liveData.getSessionDuration() < getDuration()) {
            liveData.incrementTime();
        }
    }

    @Override
    public LocalDate getCurrentLocalDate() {
        return startDate.plusDays(liveData.getSessionDuration() - 1);
    }

    public String getPrettyCurrentDate() {
        LocalDate currentDate = getCurrentLocalDate();
        long days = liveData.getSessionDuration();
        int years = (currentDate.getYear()-startDate.getYear());
        int months = currentDate.getMonth().getValue() + years * 12;
        String formattedDate = "";
        formattedDate = (name + " - Day " + days + " - Month " + months + " - Year " + (years+1) + " - " + currentDate);
        return formattedDate;
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
