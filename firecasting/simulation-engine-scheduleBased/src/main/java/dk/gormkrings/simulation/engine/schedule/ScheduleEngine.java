package dk.gormkrings.simulation.engine.schedule;

import dk.gormkrings.data.ILiveData;
import dk.gormkrings.engine.schedule.ISchedule;
import dk.gormkrings.engine.schedule.IScheduleEngine;
import dk.gormkrings.engine.schedule.IScheduleEvent;
import dk.gormkrings.engine.schedule.IScheduleFactory;
import dk.gormkrings.factory.IResultFactory;
import dk.gormkrings.factory.ISnapshotFactory;
import dk.gormkrings.phase.ICallPhase;
import dk.gormkrings.result.IResult;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.List;

@Setter
@Getter
@Component
public class ScheduleEngine implements IScheduleEngine {
    private IResultFactory resultFactory;
    private ISnapshotFactory snapshotFactory;
    private static IScheduleFactory scheduleFactory;

    public ScheduleEngine(IResultFactory resultFactory, ISnapshotFactory snapshotFactory, IScheduleFactory scheduleFactory) {
        this.resultFactory = resultFactory;
        this.snapshotFactory = snapshotFactory;
        ScheduleEngine.scheduleFactory = scheduleFactory;
    }

    public IResult simulatePhases(List<ICallPhase> phaseCopies) {
        ISchedule schedule = scheduleFactory.build(phaseCopies);
        IResult result = resultFactory.newResult();
        ICallPhase currentPhase = phaseCopies.removeFirst();
        result.addSnapshot(snapshotFactory.snapshot((ILiveData) currentPhase.getLiveData()));

        for (IScheduleEvent event : schedule.getEvents()) {
            switch (event.getType()) {
                case DAY_START:
                    currentPhase.onDayStart();
                    break;
                case MONTH_START:
                    currentPhase.onMonthStart();
                    break;
                case MONTH_END:
                    long time = event.getEpoch() - (currentPhase.getStartDate().getEpochDay() + currentPhase.getLiveData().getSessionDuration() - 1);
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
                    result.addSnapshot(snapshotFactory.snapshot((ILiveData) currentPhase.getLiveData()));
                    if (!phaseCopies.isEmpty()) {
                        currentPhase = phaseCopies.removeFirst();
                        currentPhase.getLiveData().resetSession();
                    }
                    break;
            }
        }
        return result;
    }
}

