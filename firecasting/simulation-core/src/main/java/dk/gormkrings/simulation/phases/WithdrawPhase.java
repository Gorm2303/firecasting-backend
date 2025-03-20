package dk.gormkrings.simulation.phases;

import dk.gormkrings.Withdraw;
import dk.gormkrings.data.LiveData;
import dk.gormkrings.event.Type;
import dk.gormkrings.event.date.MonthEvent;
import dk.gormkrings.taxes.TaxRule;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Getter
@Setter
public class WithdrawPhase extends SimulationPhase {
    private Withdraw withdraw;
    private boolean firstTime = true;
    private double oldCapital;
    private double oldAmount;

    public WithdrawPhase(Phase precedingPhase, long durationInMonths, Withdraw withdraw, TaxRule taxRule) {
        System.out.println("Initializing Withdraw Phase");
        setName("Withdraw");

        LocalDate startDate = precedingPhase.getStartDate().plusDays(precedingPhase.getDuration());
        LocalDate endDate = startDate.plusMonths(durationInMonths);
        int days = (int) startDate.until(endDate, ChronoUnit.DAYS);
        
        this.setStartDate(startDate);
        this.withdraw = withdraw;
        this.setDuration(days);
        this.setLiveData(precedingPhase.getLiveData());
        this.setTaxRule(taxRule);
    }

    private WithdrawPhase(Withdraw withdraw) {
        this.withdraw = withdraw.copy();
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        MonthEvent monthEvent = (MonthEvent) event;
        if (monthEvent.getType() != Type.END) return;

        LiveData data = getLiveData();
        withdrawMoney(data);
        printPretty(data);
    }

    public void withdrawMoney(LiveData data) {
        if (firstTime) {
            firstTime = false;
        }
        oldCapital = data.getCapital();
        oldAmount = withdraw.getAmount(oldCapital);

        data.subtractFromCapital(oldAmount);
    }

    public void printPretty(LiveData data) {
        System.out.println(getPrettyCurrentDate() +
                " - Capital " + formatNumber(oldCapital) +
                " - Monthly " + formatNumber(oldAmount) +
                " - New Capital " + formatNumber(data.getCapital()));
    }

    @Override
    public Phase copy() {
        return new WithdrawPhase(
                this.withdraw
        );
    }
}
