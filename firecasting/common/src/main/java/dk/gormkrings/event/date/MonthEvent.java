package dk.gormkrings.event.date;

import dk.gormkrings.data.LiveData;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

public class MonthEvent extends ApplicationEvent implements SimulationMonthEvent {
    private final LiveData data;
    @Getter
    private final Type type;

    public MonthEvent(Object source, LiveData data, Type type) {
        super(source);
        this.data = data;
        this.type = type;
    }

    @Override
    public LiveData getData() {
        return data;
    }
}
