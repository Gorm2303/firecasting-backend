package dk.gormkrings.event.date;

import dk.gormkrings.data.LiveData;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

public class YearEvent extends ApplicationEvent implements SimulationYearEvent {
    private final LiveData data;
    @Getter
    private final Type type;

    public YearEvent(Object source, LiveData liveData, Type type) {
        super(source);
        this.data = liveData;
        this.type = type;
    }

    @Override
    public LiveData getData() {
        return data;
    }
}
