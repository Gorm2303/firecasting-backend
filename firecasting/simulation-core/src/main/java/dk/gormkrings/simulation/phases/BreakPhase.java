package dk.gormkrings.simulation.phases;

import dk.gormkrings.Deposit;
import dk.gormkrings.data.LiveData;
import dk.gormkrings.event.date.MonthEvent;
import dk.gormkrings.event.Type;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;
import org.springframework.stereotype.Component;

@Getter
@Setter
public class BreakPhase extends SimulationPhase {
    private Deposit deposit;

    public BreakPhase() {
        setName("Break Phase");
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        MonthEvent monthEvent = (MonthEvent) event;
        if (monthEvent.getType() != Type.END) return;
        LiveData liveData = getLiveData();

        System.out.println(getPrettyCurrentDate() + " Break Money: " + liveData.getCapital());
    }

}
