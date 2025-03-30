package dk.gormkrings.phase;

import dk.gormkrings.action.Passive;
import dk.gormkrings.data.ILiveData;

public interface IPassivePhase {
    Passive getPassive();
    ILiveData getLiveData();

    default void calculatePassive() {
        double passiveReturn = getLiveData().getReturned() - getPassive().getPreviouslyReturned();
        getPassive().setPreviouslyReturned(getLiveData().getReturned());
        getLiveData().setPassiveReturn(passiveReturn);
        getLiveData().addToPassiveReturned(passiveReturn);
    }

    default void initializePreviouslyReturned() {
        getPassive().setPreviouslyReturned(getLiveData().getReturned());
        double passiveReturn = getLiveData().getCurrentReturn();
        getLiveData().setPassiveReturn(passiveReturn);
        getLiveData().addToPassiveReturned(passiveReturn);

    }
}
