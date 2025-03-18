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
import java.time.temporal.ChronoUnit;

@Component
public class Engine {
    private final ApplicationEventPublisher eventPublisher;
    // Define the simulation start date.
    private final LocalDate startDate = LocalDate.of(2025, 1, 1);

    public Engine(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    private int yearsToDays(int years) {
        LocalDate inYears = startDate.plusYears(years);
        return (int) startDate.until(inYears, ChronoUnit.DAYS);
    }

    public void runSimulation() {
        int runningDuration = yearsToDays(10);
        Result result = new Result();
        LiveData liveData = new LiveData(runningDuration, 1000);
        System.out.println("Simulation running for " + runningDuration + " days");
        result.addSnapshot(new Snapshot(liveData));

        while (liveData.isLive()) {
            liveData.incrementTime();
            int currentTime = liveData.getCurrentTimeSpan();
            LocalDate currentDate = startDate.plusDays(currentTime - 1);

            if (currentDate.getDayOfMonth() == 1 && !currentDate.equals(startDate)) {
                eventPublisher.publishEvent(new MonthEvent(this, liveData, Type.START));
            }

            if (currentDate.getDayOfYear() == 1 && !currentDate.equals(startDate)) {
                eventPublisher.publishEvent(new YearEvent(this, liveData, Type.START));
            }

            eventPublisher.publishEvent(new DayEvent(this, liveData));

            if (currentDate.getDayOfMonth() == currentDate.lengthOfMonth()) {
                eventPublisher.publishEvent(new MonthEvent(this, liveData, Type.END));
            }

            if (currentDate.getDayOfYear() == currentDate.lengthOfYear()) {
                eventPublisher.publishEvent(new YearEvent(this, liveData, Type.END));
            }

        }
        result.addSnapshot(new Snapshot(liveData));
        result.print();
    }
}
