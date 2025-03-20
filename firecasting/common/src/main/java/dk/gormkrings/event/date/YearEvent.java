package dk.gormkrings.event.date;

import dk.gormkrings.data.LiveData;
import dk.gormkrings.event.Type;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class YearEvent extends ApplicationEvent implements SimulationYearEvent {
    private final LiveData data;
    private final Type type;

    public YearEvent(Object source, LiveData liveData, Type type) {
        super(source);
        this.data = liveData;
        this.type = type;
    }
}
