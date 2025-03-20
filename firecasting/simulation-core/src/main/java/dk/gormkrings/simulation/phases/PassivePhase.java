package dk.gormkrings.simulation.phases;

import dk.gormkrings.data.LiveData;
import dk.gormkrings.event.date.MonthEvent;
import dk.gormkrings.event.Type;
import dk.gormkrings.taxes.TaxRule;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

@Getter
@Setter
public class PassivePhase extends SimulationPhase {
    private boolean firstTime = true;

    public PassivePhase(Phase precedingPhase, LocalDate startDate, long days, TaxRule taxRule) {
        super(precedingPhase.getLiveData(), startDate, days, taxRule,"Passive");
        System.out.println("Initializing Passive Phase");
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

    @Override
    public Phase copy() {
        return null;
    }
}
