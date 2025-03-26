package dk.gormkrings.simulation.phases.normal;

import dk.gormkrings.simulation.phases.Phase;

public interface CallPhase extends Phase {
    void onDay();
    void onWeekStart();
    void onWeekEnd();
    void onMonthStart();
    void onMonthEnd();
    void onYearStart();
    void onYearEnd();

}
