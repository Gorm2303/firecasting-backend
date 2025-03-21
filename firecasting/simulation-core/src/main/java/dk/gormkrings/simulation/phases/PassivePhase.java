package dk.gormkrings.simulation.phases;

import dk.gormkrings.data.LiveData;
import dk.gormkrings.event.date.MonthEvent;
import dk.gormkrings.event.Type;
import dk.gormkrings.taxes.TaxRule;
import lombok.NonNull;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

public class PassivePhase extends SimulationPhase {

    public PassivePhase(Phase previousPhase, LocalDate startDate, long duration, TaxRule taxRule) {
        super(previousPhase.getLiveData(), startDate, duration, taxRule, previousPhase.getReturner(), "Passive");
        System.out.println("Initializing Passive Phase");
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        MonthEvent monthEvent = (MonthEvent) event;
        if (monthEvent.getType() != Type.END) return;

        LiveData data = getLiveData();
        addReturn(data);
        System.out.println(prettyString(data));
    }


    public String prettyString(LiveData data) {
        return super.prettyString(data);
    }

    @Override
    public Phase copy(Phase previousPhase) {
        return new PassivePhase(
                previousPhase,
                getStartDate(),
                getDuration(),
                getTaxRule().copy()
        );
    }
}
