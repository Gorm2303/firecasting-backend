package dk.gormkrings.simulation.engine.event;

import dk.gormkrings.data.IDate;
import dk.gormkrings.data.ILiveData;
import dk.gormkrings.engine.IEngine;
import dk.gormkrings.event.RunEvent;
import dk.gormkrings.event.Type;
import dk.gormkrings.event.DayEvent;
import dk.gormkrings.event.MonthEvent;
import dk.gormkrings.event.YearEvent;
import dk.gormkrings.factory.IDateFactory;
import dk.gormkrings.factory.IResultFactory;
import dk.gormkrings.factory.ISnapshotFactory;
import dk.gormkrings.phase.IEventPhase;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.result.IResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service("eventEngine")
public class EventEngine implements IEngine {

    private final IDateFactory dateFactory;
    private final IResultFactory resultFactory;
    private final ISnapshotFactory snapshotFactory;

    public EventEngine(IDateFactory dateFactory, IResultFactory resultFactory,ISnapshotFactory snapshotFactory) {
        this.dateFactory = dateFactory;
        this.resultFactory = resultFactory;
        this.snapshotFactory = snapshotFactory;
    }

    public IResult simulatePhases(List<IPhase> phaseCopies) {
        IResult result = resultFactory.newResult();
        result.addSnapshot(snapshotFactory.snapshot((ILiveData) phaseCopies.getFirst().getLiveData()));
        for (IPhase phase : phaseCopies) {
            result.addResult(simulatePhase((IEventPhase) phase));
        }
        return result;
    }

    private IResult simulatePhase(IEventPhase phase) {
        log.debug("Simulation running for {} days", phase.getDuration());
        IResult result = resultFactory.newResult();
        ILiveData data = (ILiveData) phase.getLiveData();
        IDate startDate = phase.getStartDate();
        EventDispatcher dispatcher = new EventDispatcher(new SimpleApplicationEventMulticaster());
        dispatcher.register(phase);

        RunEvent simStart = new RunEvent(this, Type.START);
        dispatcher.notifyListeners(simStart);

        // Precompute boundaries using epoch day values.
        int currentEpochDay = startDate.getEpochDay() - 1;
        final int startEpochDay = currentEpochDay;
        // Compute final epoch day from phase duration.
        int finalEpochDay = (int) (startEpochDay + phase.getDuration());

        int nextMonthStartEpochDay = startDate.computeNextMonthStart();
        int currentMonthEndEpochDay = startDate.computeMonthEnd();
        int nextYearStartEpochDay = startDate.computeNextYearStart();
        int currentYearEndEpochDay = startDate.computeYearEnd();

        // Create reusable event objects.
        DayEvent dayEvent = new DayEvent(this);
        MonthEvent monthEventStart = new MonthEvent(this, Type.START);
        MonthEvent monthEventEnd = new MonthEvent(this, Type.END);
        YearEvent yearEventStart = new YearEvent(this, Type.START);
        YearEvent yearEventEnd = new YearEvent(this, Type.END);

        // Main simulation loop – controlled by epoch day.
        while (currentEpochDay < finalEpochDay) {
            data.incrementTime();
            currentEpochDay++; // advance one day

            // Publish Day Event.
            //dispatcher.notifyListeners(dayEvent);

            // Publish Month Start Event when the current day equals the precomputed boundary.
            if (currentEpochDay == nextMonthStartEpochDay && currentEpochDay != startEpochDay) {
                dispatcher.notifyListeners(monthEventStart);
                // Update boundary for next month start.
                IDate newCurrentDate = dateFactory.fromEpochDay(currentEpochDay);
                nextMonthStartEpochDay = newCurrentDate.computeNextMonthStart();
            }

            // Publish Month End Event.
            if (currentEpochDay == currentMonthEndEpochDay) {
                dispatcher.notifyListeners(monthEventEnd);
                // Update boundary for month end.
                IDate nextDay = dateFactory.fromEpochDay(currentEpochDay);
                currentMonthEndEpochDay = nextDay.computeNextMonthEnd();
            }

            // Publish Year Start Event.
            if (currentEpochDay == nextYearStartEpochDay && currentEpochDay != startEpochDay) {
                dispatcher.notifyListeners(yearEventStart);
                IDate newCurrentDate = dateFactory.fromEpochDay(currentEpochDay);
                nextYearStartEpochDay = newCurrentDate.computeNextYearStart();
            }

            // Publish Year End Event.
            if (currentEpochDay == currentYearEndEpochDay) {
                dispatcher.notifyListeners(yearEventEnd);
                IDate nextDay = dateFactory.fromEpochDay(currentEpochDay);
                currentYearEndEpochDay = nextDay.computeNextYearEnd();
            }
        }

        RunEvent simEnd = new RunEvent(this, Type.END);
        dispatcher.notifyListeners(simEnd);

        result.addSnapshot(snapshotFactory.snapshot(data));
        data.resetSession();
        dispatcher.clearRegistrations();
        return result;
    }

    @Override
    public void init(List<IPhase> phases) {

    }
}

