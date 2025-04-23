package dk.gormkrings.action;

public interface IDeposit extends IAction {
    double getMonthly();
    void setMonthly(double monthly);
    double getInitial();
    double getYearlyIncreaseInPercent();
}
