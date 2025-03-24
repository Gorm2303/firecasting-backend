package dk.gormkrings.event.date;

import dk.gormkrings.data.Live;
import dk.gormkrings.event.SimulationUpdateEvent;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class DayEvent extends ApplicationEvent implements SimulationUpdateEvent {
    private final Live data;

    public DayEvent(Object source, Live data) {
        super(source);
        this.data = data;
    }
}
