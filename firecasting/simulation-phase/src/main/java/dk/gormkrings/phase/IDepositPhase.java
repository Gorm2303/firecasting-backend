package dk.gormkrings.phase;

import dk.gormkrings.action.Deposit;
import dk.gormkrings.data.ILiveData;

public interface IDepositPhase extends ISimulationPhase {
    Deposit getDeposit();
    ILiveData getLiveData();
    boolean isFirstTime();
    void setFirstTime(boolean firstTime);

    default void depositMoney() {
        if (isFirstTime()) {
            getLiveData().addToDeposited(getDeposit().getInitial());
            getLiveData().addToCapital(getDeposit().getInitial());
            setFirstTime(false);
        }
        double depositAmount = getDeposit().getMonthly();
        getLiveData().setDeposit(depositAmount);
        getLiveData().addToDeposited(depositAmount);
        getLiveData().addToCapital(depositAmount);
        getDeposit().increaseMonthly(getDeposit().getMonthlyIncrease());
    }
}
