package dk.gormkrings.event.action;

import dk.gormkrings.event.SimulationUpdateEvent;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Getter
@Setter
public class WithdrawEvent extends ApplicationEvent implements SimulationUpdateEvent {

    public WithdrawEvent(Object source) {
        super(source);
    }
}
