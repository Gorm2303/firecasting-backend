package dk.gormkrings.data;

public interface ILiveData extends ILive {
    void addToCapital(double capital);
    void subtractFromCapital(double capital);

    void addToDeposited(double deposit);

    void addToReturned(double returned);
    void subtractFromReturned(double amount);

    void addToWithdrawn(double withdraw);
    void subtractFromWithdrawn(double amount);

    void addToPassiveReturned(double passiveReturn);
    void subtractFromPassiveReturned(double amount);

    void addToTax(double tax);
    void addToInflation(double inflation);
    void addToNetEarnings(double netEarnings);

    double getCapital();
    double getReturned();
    double getWithdraw();
    double getCurrentTax();
    double getCurrentReturn();
    double getInflation();
    long getStartTime();
    long getTotalDurationAlive();
    void setCurrentReturn(double returned);
    void setCurrentTax(double tax);
    void setDeposit(double deposit);
    void setPassiveReturn(double passiveReturn);
    void setWithdraw(double withdraw);
    void setCurrentNet(double net);
    ILiveData copy();
    String toString();
    String toCsvRow();
}
