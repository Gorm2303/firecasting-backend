package dk.gormkrings.event.date;

import dk.gormkrings.data.LiveData;
import dk.gormkrings.event.Type;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class MonthEvent extends ApplicationEvent implements SimulationMonthEvent {
    private final LiveData data;
    private final Type type;

    public MonthEvent(Object source, LiveData data, Type type) {
        super(source);
        this.data = data;
        this.type = type;
    }
}
