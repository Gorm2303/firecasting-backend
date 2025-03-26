package dk.gormkrings.simulation.engine.schedule;

import dk.gormkrings.simulation.data.Result;
import dk.gormkrings.simulation.data.Snapshot;
import dk.gormkrings.simulation.engine.Engine;
import dk.gormkrings.simulation.phases.Phase;
import dk.gormkrings.simulation.phases.normal.CallPhase;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.List;

@Setter
@Getter
@Component
public class ScheduleEngine implements Engine {
    private Schedule schedule;

    public Result simulatePhases(List<Phase> phaseCopies) {
        Result result = new Result();
        CallPhase currentPhase = (CallPhase) phaseCopies.removeFirst();
        result.addSnapshot(new Snapshot(currentPhase.getLiveData()));

        for (Event event : schedule.getEvents()) {
            switch (event.getType()) {
                case DAY_START:
                    currentPhase.onDay();
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
                    result.addSnapshot(new Snapshot(currentPhase.getLiveData()));
                    if (!phaseCopies.isEmpty()) currentPhase = (CallPhase) phaseCopies.removeFirst();
                    break;
            }
        }
        return result;
    }
}

