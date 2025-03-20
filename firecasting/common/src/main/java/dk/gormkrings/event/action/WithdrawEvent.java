package dk.gormkrings.event.action;

import dk.gormkrings.data.LiveData;
import dk.gormkrings.event.SimulationUpdateEvent;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Getter
@Setter
public class WithdrawEvent extends ApplicationEvent implements SimulationUpdateEvent {
    private LiveData data;

    public WithdrawEvent(Object source, LiveData data) {
        super(source);
        this.data = data;
    }
}
