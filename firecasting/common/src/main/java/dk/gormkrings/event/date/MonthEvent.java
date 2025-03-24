package dk.gormkrings.event.date;

import dk.gormkrings.data.Live;
import dk.gormkrings.event.Type;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class MonthEvent extends ApplicationEvent implements SimulationMonthEvent {
    private final Live data;
    private final Type type;

    public MonthEvent(Object source, Live data, Type type) {
        super(source);
        this.data = data;
        this.type = type;
    }
}
