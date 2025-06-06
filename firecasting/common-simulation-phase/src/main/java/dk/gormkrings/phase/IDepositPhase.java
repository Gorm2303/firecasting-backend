package dk.gormkrings.phase;

import dk.gormkrings.action.IDeposit;
import dk.gormkrings.data.ILiveData;

public interface IDepositPhase extends ISimulationPhase {
    IDeposit getDeposit();
    ILiveData getLiveData();

    default void depositMoney() {
        double depositAmount = getDeposit().getMonthly();
        getLiveData().setDeposit(depositAmount);
        getLiveData().addToDeposited(depositAmount);
        getLiveData().addToCapital(depositAmount);
    }

    default void increaseDeposit() {
        double newMonthly = getDeposit().getMonthly() + getDeposit().getMonthly() * (getDeposit().getYearlyIncreaseInPercent() / 100);
        getDeposit().setMonthly(newMonthly);
    }

    default void depositInitialDeposit() {
        getLiveData().addToDeposited(getDeposit().getInitial());
        getLiveData().addToCapital(getDeposit().getInitial());
    }
}
