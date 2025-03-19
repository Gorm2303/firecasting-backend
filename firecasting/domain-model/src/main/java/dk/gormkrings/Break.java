package dk.gormkrings;

import dk.gormkrings.event.date.SimulationMonthEvent;
import dk.gormkrings.event.date.Type;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Getter
@Component
public class Break implements ApplicationListener<ApplicationEvent> {
    private double total;
    private double initial;

    public Break() {
        this.initial = 0;
        this.total = initial;
    }

    public void setInitial(double initial) {
        this.initial = initial;
        total = initial;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        if (!(event instanceof SimulationMonthEvent monthEvent)) return;

        if (monthEvent.getType() != Type.END) return;

        int currentDay = monthEvent.getData().getCurrentTimeSpan();
        System.out.println("Day " + currentDay + ": Depositing money.");
    }
}
