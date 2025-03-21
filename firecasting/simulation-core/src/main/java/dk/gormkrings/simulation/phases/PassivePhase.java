package dk.gormkrings.simulation.phases;

import dk.gormkrings.action.Passive;
import dk.gormkrings.data.LiveData;
import dk.gormkrings.event.date.MonthEvent;
import dk.gormkrings.event.Type;
import dk.gormkrings.taxes.TaxRule;
import lombok.NonNull;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

public class PassivePhase extends SimulationPhase {
    private Passive passive;
    private boolean firstTime = true;

    public PassivePhase(Phase previousPhase, LocalDate startDate, long duration, Passive passive, TaxRule taxRule) {
        super(previousPhase.getLiveData(), startDate, duration, taxRule, previousPhase.getReturner(), "Passive");
        System.out.println("Initializing Passive Phase");
        this.passive = passive;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        MonthEvent monthEvent = (MonthEvent) event;
        if (monthEvent.getType() != Type.END) return;

        LiveData data = getLiveData();
        addReturn(data);
        calculatePassive(data);
        System.out.println(prettyString(data));
    }

    private void calculatePassive(LiveData data) {
        double passiveReturn = data.getCurrentReturn();
        if (firstTime) {
            passive.setInitial(data.getReturned() - passiveReturn);
            firstTime = false;
        }
        data.setPassiveReturn(passiveReturn);
        data.addToPassiveReturned(passiveReturn);
    }

    public String prettyString(LiveData data) {
        return super.prettyString(data) +
                " - Passive " + formatNumber(data.getPassiveReturned());
    }

    @Override
    public Phase copy(Phase previousPhase) {
        return new PassivePhase(
                previousPhase,
                getStartDate(),
                getDuration(),
                this.passive.copy(),
                getTaxRule().copy()
        );
    }
}
