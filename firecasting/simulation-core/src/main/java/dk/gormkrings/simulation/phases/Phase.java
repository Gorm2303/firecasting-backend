package dk.gormkrings.simulation.phases;

import dk.gormkrings.data.LiveData;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.time.LocalDate;
import java.util.List;

public interface Phase extends ApplicationListener<ApplicationEvent> {
    LiveData getLiveData();
    LocalDate getStartDate();
    long getDuration();
    String getName();
    List<ApplicationListener<ApplicationEvent>> getListeners();
    void incrementTime();
}
