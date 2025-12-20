package dk.gormkrings.returns;

public interface IReturner {
    double calculateReturn(double amount);

    /**
     * Optional lifecycle hook invoked at the end of a month.
     * Default is no-op for backwards compatibility.
     */
    default void onMonthEnd() {
        // no-op
    }

    IReturner copy();
}
