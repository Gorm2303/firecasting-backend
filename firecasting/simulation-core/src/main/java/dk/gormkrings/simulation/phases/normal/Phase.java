package dk.gormkrings.simulation.phases.normal;

import dk.gormkrings.data.Live;
import dk.gormkrings.simulation.specification.Spec;
import dk.gormkrings.util.Date;

public interface Phase {
    Date getStartDate();
    long getDuration();
    Phase copy(Spec specificationCopy);
    Spec getSpecification();
    Live getLiveData();
    void onMonthStart();
    void onMonthEnd();
    void onYearStart();
    void onYearEnd();

}
