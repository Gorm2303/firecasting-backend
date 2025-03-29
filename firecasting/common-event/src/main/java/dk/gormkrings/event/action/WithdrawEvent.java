package dk.gormkrings.event.action;

import dk.gormkrings.event.IEvent;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Getter
@Setter
public class WithdrawEvent extends ApplicationEvent implements IEvent {

    public WithdrawEvent(Object source) {
        super(source);
    }
}
