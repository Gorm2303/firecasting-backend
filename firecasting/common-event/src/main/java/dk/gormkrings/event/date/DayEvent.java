package dk.gormkrings.event.date;

import dk.gormkrings.event.SimulationUpdateEvent;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class DayEvent extends ApplicationEvent implements SimulationUpdateEvent {

    public DayEvent(Object source) {
        super(source);
    }
}
