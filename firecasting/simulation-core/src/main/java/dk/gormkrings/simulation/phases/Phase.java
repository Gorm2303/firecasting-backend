package dk.gormkrings.simulation.phases;

import dk.gormkrings.data.LiveData;
import dk.gormkrings.inflation.Inflation;
import dk.gormkrings.returns.Return;
import dk.gormkrings.taxes.TaxRule;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.time.LocalDate;

public interface Phase extends ApplicationListener<ApplicationEvent> {
    LiveData getLiveData();
    LocalDate getStartDate();
    long getDuration();
    String getName();
    void incrementTime();
    LocalDate getCurrentLocalDate();
    TaxRule getTaxRule();
    Phase copy(Phase previousPhase);
    Return getReturner();
    void addReturn(LiveData data);
    Inflation getInflation();
}
