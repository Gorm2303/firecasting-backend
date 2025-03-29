package dk.gormkrings.phase.eventBased;

import dk.gormkrings.phase.ISimulationPhase;
import dk.gormkrings.data.IDate;
import dk.gormkrings.data.ILiveData;
import dk.gormkrings.event.Type;
import dk.gormkrings.event.MonthEvent;
import dk.gormkrings.event.YearEvent;
import dk.gormkrings.phase.IEventPhase;
import dk.gormkrings.specification.ISpecification;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

@Slf4j
@Getter
@Setter
public abstract class SimulationEventPhase implements IEventPhase, ISimulationPhase {
    private IDate startDate;
    private long duration;
    private ISpecification specification;
    private String name;

    SimulationEventPhase(ISpecification specification, IDate startDate, long duration, String name) {
        this.startDate = startDate;
        this.duration = duration;
        this.specification = specification;
        this.name = name;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        if (event instanceof MonthEvent monthEvent &&
                monthEvent.getType() == Type.END) {
            addReturn();
        } else if (event instanceof YearEvent yearEvent &&
                yearEvent.getType() == Type.END) {
            addTax();
            addInflation();
        }
    }

    @Override
    public ILiveData getLiveData() {
        return (ILiveData) specification.getLiveData();
    }

    public String prettyString() {
        return name + getLiveData().toString();
    }

    @Override
    public boolean supportsEventType(@NonNull Class<? extends ApplicationEvent> eventType) {
        return MonthEvent.class.isAssignableFrom(eventType) || YearEvent.class.isAssignableFrom(eventType);
    }

    @Override
    public boolean supportsSourceType(Class<?> sourceType) {
        return true;
    }
}
