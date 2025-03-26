package dk.gormkrings.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class RunEvent extends ApplicationEvent implements SimulationUpdateEvent {
    private final Type type;

    public RunEvent(Object source, Type type) {
        super(source);
        this.type = type;
    }
}
