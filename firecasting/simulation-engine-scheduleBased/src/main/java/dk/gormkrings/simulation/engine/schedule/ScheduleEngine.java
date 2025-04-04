package dk.gormkrings.simulation.engine.schedule;

import dk.gormkrings.data.ILiveData;
import dk.gormkrings.engine.IEngine;
import dk.gormkrings.engine.schedule.ISchedule;
import dk.gormkrings.engine.schedule.IScheduleEvent;
import dk.gormkrings.engine.schedule.IScheduleFactory;
import dk.gormkrings.factory.IResultFactory;
import dk.gormkrings.factory.ISnapshotFactory;
import dk.gormkrings.phase.ICallPhase;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.result.IResult;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.List;

@Setter
@Getter
@Service("scheduleEngine")
@Scope("prototype")
public class ScheduleEngine implements IEngine {
    private IResultFactory resultFactory;
    private ISnapshotFactory snapshotFactory;
    private static IScheduleFactory scheduleFactory;

    public ScheduleEngine(IResultFactory resultFactory, ISnapshotFactory snapshotFactory, IScheduleFactory scheduleFactory) {
        this.resultFactory = resultFactory;
        this.snapshotFactory = snapshotFactory;
        ScheduleEngine.scheduleFactory = scheduleFactory;
    }

    @Override
    public IResult simulatePhases(List<IPhase> phaseCopies) {
        ISchedule schedule = scheduleFactory.getSchedule();
        IResult result = resultFactory.newResult();
        ICallPhase currentPhase = (ICallPhase) phaseCopies.removeFirst();
        boolean simStart = true;

        for (IScheduleEvent event : schedule.getEvents()) {
            switch (event.getType()) {
                case DAY_START:
                    currentPhase.onDayStart();
                    break;
                case DAY_END:
                    currentPhase.getLiveData().incrementTime();
                    currentPhase.onDayEnd();
                    break;
                case WEEK_START:
                    currentPhase.onWeekStart();
                    break;
                case WEEK_END:
                    currentPhase.onWeekEnd();
                    break;
                case MONTH_START:
                    currentPhase.onMonthStart();
                    break;
                case MONTH_END:
                    currentPhase.onMonthEnd();
                    break;
                case YEAR_START:
                    currentPhase.onYearStart();
                    break;
                case YEAR_END:
                    currentPhase.onYearEnd();
                    result.addSnapshot(snapshotFactory.snapshot((ILiveData) currentPhase.getLiveData()));
                    break;
                case PHASE_START:
                    currentPhase.onPhaseStart();
                    if (simStart) {
                        result.addSnapshot(snapshotFactory.snapshot((ILiveData) currentPhase.getLiveData()));
                        simStart = false;
                    }
                    break;
                case PHASE_END:
                    currentPhase.onPhaseEnd();
                    if (!phaseCopies.isEmpty()) {
                        currentPhase = (ICallPhase) phaseCopies.removeFirst();
                        currentPhase.getLiveData().resetSession();
                    }
                    break;
            }
        }
        return result;
    }

    @Override
    public void init(List<IPhase> phases) {
        scheduleFactory.build(phases);
    }
}

