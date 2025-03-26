package dk.gormkrings.event.date;

import dk.gormkrings.data.Live;
import dk.gormkrings.event.Type;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class YearEvent extends ApplicationEvent implements SimulationYearEvent {
    private final Live data;
    private final Type type;

    public YearEvent(Object source, Live liveData, Type type) {
        super(source);
        this.data = liveData;
        this.type = type;
    }
}
