package dk.gormkrings.action;

public interface IPassive extends IAction {
    double getPreviouslyReturned();
    void setPreviouslyReturned(double previouslyReturned);
}
