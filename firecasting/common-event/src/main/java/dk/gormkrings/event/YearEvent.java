package dk.gormkrings.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class YearEvent extends ApplicationEvent implements IYearEvent {
    private final Type type;

    public YearEvent(Object source, Type type) {
        super(source);
        this.type = type;
    }
}
