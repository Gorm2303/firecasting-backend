package dk.gormkrings.simulation.phases;

import dk.gormkrings.data.LiveData;
import dk.gormkrings.event.date.MonthEvent;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;

import java.time.LocalDate;

@Getter
@Setter
public abstract class SimulationPhase implements Phase, SmartApplicationListener {
    private LiveData liveData;
    private String name;
    private LocalDate startDate;
    private long duration;

    @Override
    public void incrementTime() {
        if (liveData.getCurrentTimeSpan() < getDuration()) {
            liveData.incrementTime();
        }
    }

    @Override
    public LocalDate getCurrentLocalDate() {
        return startDate.plusDays(liveData.getCurrentTimeSpan() - 1);
    }

    public String getPrettyCurrentDate() {
        LocalDate currentDate = getCurrentLocalDate();
        int days = liveData.getCurrentTimeSpan();
        int years = (currentDate.getYear()-startDate.getYear());
        int months = currentDate.getMonth().getValue() + years * 12;
        String formattedDate = "";
        formattedDate = ("Day " + days + " - Month " + months + " - Year " + (years+1) + " - Date: " + currentDate);
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

}
