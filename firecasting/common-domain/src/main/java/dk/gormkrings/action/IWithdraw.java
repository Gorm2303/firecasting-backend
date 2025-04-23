package dk.gormkrings.action;

public interface IWithdraw extends IAction {
    double getMonthlyAmount(double capital, double inflation);
    double getDynamicAmountOfReturn();
    double getLowerVariationPercentage();
    double getUpperVariationPercentage();
    void setDynamicAmountOfReturn(double dynamicAmountOfReturn);
}
