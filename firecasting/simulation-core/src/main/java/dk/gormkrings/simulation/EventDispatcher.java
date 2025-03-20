package dk.gormkrings.simulation;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.stereotype.Component;

@Component
public class EventDispatcher {

    private final ApplicationEventMulticaster multicaster;

    public EventDispatcher(ApplicationEventMulticaster multicaster) {
        this.multicaster = multicaster;
    }

    public void register(ApplicationListener<?> listener) {
        multicaster.addApplicationListener(listener);
    }

    public void unregister(ApplicationListener<?> listener) {
        multicaster.removeApplicationListener(listener);
    }

    public void clearRegistrations() {
        multicaster.removeAllListeners();
    }

    public void notifyListeners(ApplicationEvent event) {
        multicaster.multicastEvent(event);
    }
}
