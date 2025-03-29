package dk.gormkrings.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class MonthEvent extends ApplicationEvent implements IMonthEvent {
    private final Type type;

    public MonthEvent(Object source, Type type) {
        super(source);
        this.type = type;
    }
}
