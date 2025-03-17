package dk.gormkrings.simulation;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class DayEvent extends ApplicationEvent implements SimulationUpdateEvent {
    private final int day;
    private final LiveData data;

    public DayEvent(Object source, int day, LiveData data) {
        super(source);
        this.day = day;
        this.data = data;
    }
}
