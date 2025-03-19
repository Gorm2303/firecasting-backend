package dk.gormkrings.simulation.phases;

import dk.gormkrings.data.LiveData;
import dk.gormkrings.event.date.MonthEvent;
import dk.gormkrings.event.Type;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.text.DecimalFormat;

@Getter
@Setter
public class PassivePhase extends SimulationPhase {
    private boolean firstTime = true;

    public PassivePhase() {
        setName("Passive");
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        MonthEvent monthEvent = (MonthEvent) event;
        if (monthEvent.getType() != Type.END) return;

        LiveData data = getLiveData();
        monthlyUpdate(data);
        printPretty(data);
    }

    public void monthlyUpdate(LiveData data) {
        if (firstTime) {
            firstTime = false;
        }
    }

    public void printPretty(LiveData data) {
        System.out.println(getPrettyCurrentDate() +
                " - Passive Earnings " + formatNumber(data.getPassiveMoney()) +
                " - Capital " + formatNumber(data.getCapital()));
    }
}
