package dk.gormkrings.event.date;

import dk.gormkrings.event.Type;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class YearEvent extends ApplicationEvent implements SimulationYearEvent {
    private final Type type;

    public YearEvent(Object source, Type type) {
        super(source);
        this.type = type;
    }
}
