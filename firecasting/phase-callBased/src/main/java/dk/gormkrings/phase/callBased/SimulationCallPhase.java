package dk.gormkrings.phase.callBased;

import dk.gormkrings.phase.ISimulationPhase;
import dk.gormkrings.data.IDate;
import dk.gormkrings.data.ILiveData;
import dk.gormkrings.event.EventType;
import dk.gormkrings.phase.ICallPhase;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.ITaxExemption;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Getter
@Setter
public abstract class SimulationCallPhase implements ICallPhase, ISimulationPhase {
    private IDate startDate;
    private long duration;
    private ISpecification specification;
    private List<ITaxExemption> taxExemptions;
    private String name;

    public SimulationCallPhase(ISpecification specification, IDate startDate, List<ITaxExemption> taxExemptions, long duration, String name) {
        this.startDate = startDate;
        this.duration = duration;
        this.specification = specification;
        this.taxExemptions = taxExemptions;
        this.name = name;
    }

    @Override
    public boolean supportsEvent(EventType eventType) {
        return eventType.equals(EventType.DAY_END)
                || eventType.equals(EventType.YEAR_START)
                || eventType.equals(EventType.YEAR_END);
    }

    @Override
    public void onDayStart() {

    }

    @Override
    public void onDayEnd() {
        if (getLiveData().getCapital() <= 0) return;
        if (startDate.plusDays(specification.getLiveData().getSessionDuration()).getDayOfWeek() < 5) addReturn();
    }

    @Override
    public void onWeekStart() {

    }

    @Override
    public void onWeekEnd() {

    }

    @Override
    public void onMonthStart() {

    }

    @Override
    public void onMonthEnd() {

    }

    @Override
    public void onYearStart() {
        for (ITaxExemption rule : getTaxExemptions()) {
            rule.yearlyUpdate();
        }
    }

    @Override
    public void onYearEnd() {
        addNotionalTax();
        compoundInflation();
    }

    @Override
    public void onPhaseEnd() {

    }

    @Override
    public void onPhaseStart() {
        getLiveData().setPhaseName(name);
    }

    @Override
    public ILiveData getLiveData() {
        return (ILiveData) specification.getLiveData();
    }

    public String prettyString() {
        return getLiveData().toString();
    }

}
