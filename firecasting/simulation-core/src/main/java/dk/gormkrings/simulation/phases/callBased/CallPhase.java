package dk.gormkrings.simulation.phases.callBased;

import dk.gormkrings.simulation.engine.schedule.EventType;
import dk.gormkrings.simulation.phases.Phase;

public interface CallPhase extends Phase {
    void onDayStart();
    void onDayEnd();
    void onWeekStart();
    void onWeekEnd();
    void onMonthStart();
    void onMonthEnd();
    void onYearStart();
    void onYearEnd();
    boolean supportsEvent(EventType eventType);
}
