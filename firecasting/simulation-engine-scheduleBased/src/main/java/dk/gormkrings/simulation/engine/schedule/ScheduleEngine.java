package dk.gormkrings.simulation.engine.schedule;

import dk.gormkrings.data.ILiveData;
import dk.gormkrings.engine.IEngine;
import dk.gormkrings.event.IEvent;
import dk.gormkrings.phase.ICallPhase;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.result.IResult;
import dk.gormkrings.simulation.result.Result;
import dk.gormkrings.simulation.result.Snapshot;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.List;

@Setter
@Getter
@Component
public class ScheduleEngine implements IEngine {
    private Schedule schedule;

    public IResult simulatePhases(List<IPhase> phaseCopies) {
        IResult result = new Result();
        ICallPhase currentPhase = (ICallPhase) phaseCopies.removeFirst();
        result.addSnapshot(new Snapshot((ILiveData) currentPhase.getLiveData()));

        for (IEvent event : schedule.getEvents()) {
            switch (event.getType()) {
                case DAY_START:
                    currentPhase.onDayStart();
                    break;
                case MONTH_START:
                    currentPhase.onMonthStart();
                    break;
                case MONTH_END:
                    long time = event.getEpochDay() - (currentPhase.getStartDate().getEpochDay() + currentPhase.getLiveData().getSessionDuration() - 1);
                    currentPhase.getLiveData().incrementTime(time);
                    currentPhase.onMonthEnd();
                    break;
                case YEAR_START:
                    currentPhase.onYearStart();
                    break;
                case YEAR_END:
                    currentPhase.onYearEnd();
                    break;
                case PHASE_SWITCH:
                    result.addSnapshot(new Snapshot((ILiveData) currentPhase.getLiveData()));
                    if (!phaseCopies.isEmpty()) {
                        currentPhase = (ICallPhase) phaseCopies.removeFirst();
                        currentPhase.getLiveData().resetSession();
                    }
                    break;
            }
        }
        return result;
    }
}

