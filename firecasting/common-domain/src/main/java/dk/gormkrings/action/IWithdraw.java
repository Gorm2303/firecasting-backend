package dk.gormkrings.action;

public interface IWithdraw extends IAction {
    double getMonthlyAmount(double capital, double inflation);
    double getDynamicPercentOfReturn();
    void setDynamicAmountOfReturn(double dynamicAmountOfReturn);
}
