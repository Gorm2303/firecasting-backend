package dk.gormkrings.engine.schedule;

import dk.gormkrings.phase.IPhase;

import java.util.List;

public interface IScheduleFactory {
    ISchedule build(List<IPhase> phases);
    ISchedule getSchedule();
}
