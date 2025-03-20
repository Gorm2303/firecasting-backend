package dk.gormkrings.event;

import dk.gormkrings.data.LiveData;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class RunEvent extends ApplicationEvent implements SimulationUpdateEvent {
    private final Type type;
    private final LiveData data;

    public RunEvent(Object source, LiveData data, Type type) {
        super(source);
        this.data = data;
        this.type = type;
    }
}
