package dk.gormkrings.action;

public interface IActionFactory {
    IAction createDepositAction(Double initial, Double monthly, Double yearlyIncrease);
    IAction createWithdrawAction(Double amount, Double rate, Double lowerVariation, Double upperVariation);
    IAction createPassiveAction();
}
