package dk.gormkrings.simulation.phases;

import dk.gormkrings.data.LiveData;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public abstract class SimulationPhase implements Phase {
    private LiveData liveData;
    private String name;
    private LocalDate startDate;
    private List<ApplicationListener<ApplicationEvent>> listeners;
    private long duration;

    @Override
    public void incrementTime() {
        if (liveData.getCurrentTimeSpan() < getDuration()) {
            liveData.incrementTime();
        }
    }
}
