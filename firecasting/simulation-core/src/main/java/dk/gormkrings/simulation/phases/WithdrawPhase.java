package dk.gormkrings.simulation.phases;

import dk.gormkrings.action.Withdraw;
import dk.gormkrings.data.LiveData;
import dk.gormkrings.event.Type;
import dk.gormkrings.event.date.MonthEvent;
import dk.gormkrings.taxes.TaxRule;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

@Getter
@Setter
public class WithdrawPhase extends SimulationPhase {
    private Withdraw withdraw;
    private boolean firstTime = true;

    public WithdrawPhase(Phase previousPhase, LocalDate startDate, long duration, Withdraw withdraw, TaxRule taxRule) {
        super(previousPhase.getLiveData(), startDate, duration, taxRule, previousPhase.getReturner(), "Withdraw");
        System.out.println("Initializing Withdraw Phase");
        this.withdraw = withdraw;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        MonthEvent monthEvent = (MonthEvent) event;
        if (monthEvent.getType() != Type.END) return;

        LiveData data = getLiveData();
        addReturn(data);
        withdrawMoney(data);
        System.out.println(prettyString(data));
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
    public String prettyString(LiveData data) {
        return super.prettyString(data) +
                " - Withdraw " + formatNumber(data.getWithdraw()) +
                " - Withdrawn " + formatNumber(data.getWithdrawn());
    }

    @Override
    public Phase copy(Phase previousPhase) {
        return new WithdrawPhase(
                previousPhase,
                getStartDate(),
                getDuration(),
                this.withdraw,
                getTaxRule().copy()
        );
    }
}
