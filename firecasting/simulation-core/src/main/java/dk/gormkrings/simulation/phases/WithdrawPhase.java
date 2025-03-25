package dk.gormkrings.simulation.phases;

import dk.gormkrings.action.Withdraw;
import dk.gormkrings.data.LiveData;
import dk.gormkrings.event.Type;
import dk.gormkrings.event.date.MonthEvent;
import dk.gormkrings.simulation.specification.Spec;
import dk.gormkrings.simulation.specification.Specification;
import dk.gormkrings.util.Util;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

@Slf4j
@Getter
@Setter
public class WithdrawPhase extends SimulationPhase {
    private Withdraw withdraw;
    private boolean firstTime = true;

    public WithdrawPhase(Specification specification, LocalDate startDate, long duration, Withdraw withdraw) {
        super(specification, startDate, duration, "Withdraw");
        log.debug("Initializing Withdraw Phase: {}, for {} days", startDate, duration);
        this.withdraw = withdraw;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        MonthEvent monthEvent = (MonthEvent) event;
        if (monthEvent.getType() != Type.END) return;

        LiveData data = getLiveData();
        addReturn();
        withdrawMoney(data);
        log.debug(prettyString());
    }

    public void withdrawMoney(LiveData data) {
        if (firstTime) {
            firstTime = false;
        }
        double withdraw = this.withdraw.getMonthlyAmount(data.getCapital());
        data.setWithdraw(withdraw);
        data.addToWithdrawn(withdraw);
        data.subtractFromCapital(withdraw);
    }

    @Override
    public String prettyString() {
        return super.prettyString() +
                " - Withdraw " + Util.formatNumber(getLiveData().getWithdraw()) +
                " - Withdrawn " + Util.formatNumber(getLiveData().getWithdrawn());
    }

    @Override
    public WithdrawPhase copy(Spec specificationCopy) {
        return new WithdrawPhase(
                (Specification) specificationCopy,
                getStartDate(),
                getDuration(),
                this.withdraw.copy()
        );
    }
}
