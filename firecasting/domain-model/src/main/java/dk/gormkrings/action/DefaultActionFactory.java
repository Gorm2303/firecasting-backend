package dk.gormkrings.action;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DefaultActionFactory implements IActionFactory {

    @Override
    public IAction createDepositAction(Double initial, Double monthly, Double yearlyIncrease) {
        log.info("Creating Deposit Action");
        return new Deposit(
                defaultToZero(initial),
                defaultToZero(monthly),
                defaultToZero(yearlyIncrease)
        );
    }

    @Override
    public IAction createWithdrawAction(Double amount, Double rate, Double lowerVar, Double upperVar) {
        log.info("Creating Withdraw Action");
        return new Withdraw(
                defaultToZero(amount),
                defaultToZero(rate),
                defaultToZero(lowerVar),
                defaultToZero(upperVar)
        );
    }

    @Override
    public IAction createPassiveAction() {
        log.info("Creating Passive Action");
        return new Passive();
    }

    private double defaultToZero(Double value) {
        return value != null ? value : 0.0;
    }
}
