package dk.gormkrings.simulation;

import dk.gormkrings.data.LiveData;
import dk.gormkrings.event.RunEvent;
import dk.gormkrings.event.Type;
import dk.gormkrings.event.date.*;
import dk.gormkrings.simulation.data.Result;
import dk.gormkrings.simulation.data.Snapshot;
import dk.gormkrings.simulation.phases.Phase;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class Engine {

    public Result simulatePhases(List<Phase> phases) {
        Result result = new Result();
        for (Phase phase : phases) {
            result.addResult(simulatePhase(phase));
        }
        return result;
    }

    private Result simulatePhase(Phase phase) {
        Result result = new Result();
        LiveData data = phase.getLiveData();
        LocalDate startDate = phase.getStartDate();
        EventDispatcher dispatcher = new EventDispatcher(new SimpleApplicationEventMulticaster());
        dispatcher.register(phase);
        if (phase.getTaxRule() != null) dispatcher.register(phase.getTaxRule()) ;

        System.out.println("Simulation running for " + phase.getDuration() + " days");
        result.addSnapshot(new Snapshot(data));

        RunEvent simStart = new RunEvent(this, data, Type.START);
        dispatcher.notifyListeners(simStart);

        while (data.isLive(phase.getDuration())) {
            data.incrementTime();
            long currentTime = data.getSessionDuration();
            LocalDate currentDate = startDate.plusDays(currentTime - 1);

            // Publish Month Start Event
            if (currentDate.getDayOfMonth() == 1 && !currentDate.equals(startDate)) {
                MonthEvent monthStart = new MonthEvent(this, data, Type.START);
                dispatcher.notifyListeners(monthStart);
            }

            // Publish Year Start Event
            if (currentDate.getDayOfYear() == 1 && !currentDate.equals(startDate)) {
                YearEvent yearStart = new YearEvent(this, data, Type.START);
                dispatcher.notifyListeners(yearStart);
            }

            // Publish Day Event
            DayEvent dayEvent = new DayEvent(this, data);
            dispatcher.notifyListeners(dayEvent);

            // Publish Month End Event
            if (currentDate.getDayOfMonth() == currentDate.lengthOfMonth()) {
                MonthEvent monthEnd = new MonthEvent(this, data, Type.END);
                dispatcher.notifyListeners(monthEnd);
            }

            // Publish Year End Event
            if (currentDate.getDayOfYear() == currentDate.lengthOfYear()) {
                YearEvent yearEnd = new YearEvent(this, data, Type.END);
                dispatcher.notifyListeners(yearEnd);
            }
        }
        RunEvent simEnd = new RunEvent(this, data, Type.END);
        dispatcher.notifyListeners(simEnd);

        result.addSnapshot(new Snapshot(data));
        result.print();
        data.resetSession();
        dispatcher.clearRegistrations();
        return result;
    }
}
