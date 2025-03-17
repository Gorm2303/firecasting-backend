package dk.gormkrings;

import dk.gormkrings.simulation.DayEvent;
import dk.gormkrings.simulation.LiveData;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class Engine {
    private final ApplicationEventPublisher eventPublisher;

    public Engine(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

   public void runSimulation(LiveData liveData) {
        System.out.println("Simulation running");
        int totalDays = liveData.getDuration();
        for (int i = 0; i < totalDays; i++) {
            eventPublisher.publishEvent(new DayEvent(this, i, liveData));
        }
   }
}
