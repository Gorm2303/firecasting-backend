package dk.gormkrings.phase;

import dk.gormkrings.action.Passive;
import dk.gormkrings.data.ILiveData;

public interface IPassivePhase {
    Passive getPassive();
    ILiveData getLiveData();
    boolean isFirstTime();
    void setFirstTime(boolean firstTime);

    default void calculatePassive() {
        double passiveReturn = 0;
        if (isFirstTime()) {
            setFirstTime(false);
            getPassive().setPreviouslyReturned(getLiveData().getReturned());
            passiveReturn = getLiveData().getCurrentReturn();
        } else {
            passiveReturn = getLiveData().getReturned() - getPassive().getPreviouslyReturned();
            getPassive().setPreviouslyReturned(getLiveData().getReturned());
        }

        getLiveData().setPassiveReturn(passiveReturn);
        getLiveData().addToPassiveReturned(passiveReturn);
    }
}
