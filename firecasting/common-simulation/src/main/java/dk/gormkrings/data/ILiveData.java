package dk.gormkrings.data;

public interface ILiveData extends ILive {
    void addToCapital(double capital);
    void subtractFromCapital(double capital);

    void addToDeposited(double deposit);
    void subtractFromDeposited(double deposit);

    void addToReturned(double returned);
    void subtractFromReturned(double amount);

    void addToWithdrawn(double withdraw);
    void subtractFromWithdrawn(double amount);

    void addToPassiveReturned(double passiveReturn);
    void subtractFromPassiveReturned(double amount);

    void addToTax(double tax);

    /** Records an absolute fee amount deducted from capital (e.g., management fees). */
    void addToFee(double fee);
    void compoundInflation(double inflation);
    void addToNetEarnings(double netEarnings);

    double getCapital();
    double getDeposited();
    double getReturned();
    double getWithdraw();
    double getWithdrawn();
    double getCurrentTax();
    double getTax();
    double getFee();
    double getCurrentReturn();
    double getInflation();
    double getNet();
    double getCurrentFee();
    long getStartTime();
    long getTotalDurationAlive();
    void setCurrentReturn(double returned);
    void setCurrentTax(double tax);
    void setCurrentFee(double fee);
    void setDeposit(double deposit);
    void setPassiveReturn(double passiveReturn);
    void setWithdraw(double withdraw);
    void setCurrentNet(double net);
    void setPhaseName(String phaseName);
    ILiveData copy();
    String toString();
    String toCsvRow();
    String getPhaseName();
}
