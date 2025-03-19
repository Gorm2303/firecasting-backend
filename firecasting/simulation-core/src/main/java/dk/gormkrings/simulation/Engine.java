package dk.gormkrings.simulation;

import dk.gormkrings.data.LiveData;
import dk.gormkrings.event.RunEvent;
import dk.gormkrings.event.Type;
import dk.gormkrings.event.date.*;
import dk.gormkrings.simulation.data.Result;
import dk.gormkrings.simulation.data.Snapshot;
import dk.gormkrings.simulation.phases.Phase;
import org.springframework.context.ApplicationEvent;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class Engine {

    private final EventDispatcher dispatcher;

    public Engine(EventDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    private void notifyListeners(ApplicationEvent event) {
        dispatcher.notifyListeners(event);
    }

    public Phase runSimulation(Phase phase) {
        LiveData data = phase.getLiveData();
        LocalDate startDate = phase.getStartDate();
        dispatcher.register(phase);
        Result result = new Result();
        System.out.println("Simulation running for " + phase.getDuration() + " days");
        result.addSnapshot(new Snapshot(data));

        RunEvent simStart = new RunEvent(this, data, Type.START);
        notifyListeners(simStart);

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
        RunEvent simEnd = new RunEvent(this, data, Type.END);
        notifyListeners(simEnd);

        result.addSnapshot(new Snapshot(data));
        result.print();
        dispatcher.unregister(phase);
        return phase;
    }
}
