package dk.gormkrings.event.date;

import dk.gormkrings.data.LiveData;
import dk.gormkrings.event.SimulationUpdateEvent;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class DayEvent extends ApplicationEvent implements SimulationUpdateEvent {
    private final LiveData data;

    public DayEvent(Object source, LiveData data) {
        super(source);
        this.data = data;
    }
}
