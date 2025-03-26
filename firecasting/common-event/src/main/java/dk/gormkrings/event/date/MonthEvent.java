package dk.gormkrings.event.date;

import dk.gormkrings.event.Type;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class MonthEvent extends ApplicationEvent implements SimulationMonthEvent {
    private final Type type;

    public MonthEvent(Object source, Type type) {
        super(source);
        this.type = type;
    }
}
