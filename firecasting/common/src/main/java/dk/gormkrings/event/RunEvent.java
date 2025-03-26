package dk.gormkrings.event;

import dk.gormkrings.data.Live;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class RunEvent extends ApplicationEvent implements SimulationUpdateEvent {
    private final Type type;
    private final Live data;

    public RunEvent(Object source, Live data, Type type) {
        super(source);
        this.data = data;
        this.type = type;
    }
}
