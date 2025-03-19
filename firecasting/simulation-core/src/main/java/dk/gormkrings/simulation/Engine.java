package dk.gormkrings.simulation;

import dk.gormkrings.data.LiveData;
import dk.gormkrings.event.date.DayEvent;
import dk.gormkrings.event.date.MonthEvent;
import dk.gormkrings.event.date.Type;
import dk.gormkrings.event.date.YearEvent;
import dk.gormkrings.simulation.data.Result;
import dk.gormkrings.simulation.data.Snapshot;
import dk.gormkrings.simulation.phases.Phase;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class Engine {

    private final List<ApplicationListener<ApplicationEvent>> listeners = new ArrayList<>();

    private void notifyListeners(ApplicationEvent event) {
        for (ApplicationListener<ApplicationEvent> listener : listeners) {
            listener.onApplicationEvent(event);
        }
    }

    public void runSimulation(Phase phase) {
        LiveData data = phase.getLiveData();
        LocalDate startDate = phase.getStartDate();
        listeners.add(phase);
        Result result = new Result();
        System.out.println("Simulation running for " + phase.getDuration() + " days");
        result.addSnapshot(new Snapshot(data));

        while (data.isLive(phase.getDuration())) {
            data.incrementTime();
            int currentTime = data.getCurrentTimeSpan();
            LocalDate currentDate = startDate.plusDays(currentTime - 1);

            // Publish Month Start Event
            if (currentDate.getDayOfMonth() == 1 && !currentDate.equals(startDate)) {
                MonthEvent monthStart = new MonthEvent(this, data, Type.START);
                notifyListeners(monthStart);
            }

            // Publish Year Start Event
            if (currentDate.getDayOfYear() == 1 && !currentDate.equals(startDate)) {
                YearEvent yearStart = new YearEvent(this, data, Type.START);
                notifyListeners(yearStart);
            }

            // Publish Day Event
            DayEvent dayEvent = new DayEvent(this, data);
            notifyListeners(dayEvent);

            // Publish Month End Event
            if (currentDate.getDayOfMonth() == currentDate.lengthOfMonth()) {
                MonthEvent monthEnd = new MonthEvent(this, data, Type.END);
                notifyListeners(monthEnd);
            }

            // Publish Year End Event
            if (currentDate.getDayOfYear() == currentDate.lengthOfYear()) {
                YearEvent yearEnd = new YearEvent(this, data, Type.END);
                notifyListeners(yearEnd);
            }
        }
        result.addSnapshot(new Snapshot(data));
        result.print();
        listeners.remove(phase);
    }
}
