package dk.gormkrings.engine.schedule;

import dk.gormkrings.phase.ICallPhase;

import java.util.List;

public interface IScheduleFactory {
    ISchedule build(List<ICallPhase> phases);
}
