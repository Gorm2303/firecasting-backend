package dk.gormkrings.phase;

import dk.gormkrings.event.EventType;

public interface ICallPhase extends IPhase {
    void onDayStart();
    void onDayEnd();
    void onWeekStart();
    void onWeekEnd();
    void onMonthStart();
    void onMonthEnd();
    void onYearStart();
    void onYearEnd();
    void onPhaseStart();
    void onPhaseEnd();
    boolean supportsEvent(EventType eventType);
}
