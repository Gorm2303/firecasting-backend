package dk.gormkrings.simulation;

import dk.gormkrings.data.LiveData;
import dk.gormkrings.event.date.DayEvent;
import dk.gormkrings.event.date.MonthEvent;
import dk.gormkrings.event.date.Type;
import dk.gormkrings.event.date.YearEvent;
import dk.gormkrings.simulation.data.Result;
import dk.gormkrings.simulation.data.Snapshot;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class Engine {
    private final ApplicationEventPublisher eventPublisher;

    public Engine(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void runSimulation(LocalDate startDate, LiveData data) {
        Result result = new Result();
        System.out.println("Simulation running for " + data.getDuration() + " days");
        result.addSnapshot(new Snapshot(data));

        while (data.isLive()) {
            data.incrementTime();
            int currentTime = data.getCurrentTimeSpan();
            LocalDate currentDate = startDate.plusDays(currentTime - 1);

            if (currentDate.getDayOfMonth() == 1 && !currentDate.equals(startDate)) {
                eventPublisher.publishEvent(new MonthEvent(this, data, Type.START));
            }

            if (currentDate.getDayOfYear() == 1 && !currentDate.equals(startDate)) {
                eventPublisher.publishEvent(new YearEvent(this, data, Type.START));
            }

            eventPublisher.publishEvent(new DayEvent(this, data));

            if (currentDate.getDayOfMonth() == currentDate.lengthOfMonth()) {
                eventPublisher.publishEvent(new MonthEvent(this, data, Type.END));
            }

            if (currentDate.getDayOfYear() == currentDate.lengthOfYear()) {
                eventPublisher.publishEvent(new YearEvent(this, data, Type.END));
            }

        }
        result.addSnapshot(new Snapshot(data));
        result.print();
    }
}
