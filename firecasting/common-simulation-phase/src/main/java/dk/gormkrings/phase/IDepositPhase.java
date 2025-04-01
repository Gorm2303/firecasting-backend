package dk.gormkrings.phase;

import dk.gormkrings.action.Deposit;
import dk.gormkrings.data.ILiveData;

public interface IDepositPhase extends ISimulationPhase {
    Deposit getDeposit();
    ILiveData getLiveData();

    default void depositMoney() {
        double depositAmount = getDeposit().getMonthly();
        getLiveData().setDeposit(depositAmount);
        getLiveData().addToDeposited(depositAmount);
        getLiveData().addToCapital(depositAmount);
    }

    default void depositInitialDeposit() {
        getLiveData().addToDeposited(getDeposit().getInitial());
        getLiveData().addToCapital(getDeposit().getInitial());
    }
}
