package dk.gormkrings.regime;

public interface IRegimeProvider {
    int getCurrentRegime();

    /**
     * Optional lifecycle hook for time-based regime providers.
     *
     * <p>Default is no-op to keep backwards compatibility with providers that
     * switch regimes on-demand in {@link #getCurrentRegime()}.</p>
     */
    default void onMonthEnd() {
        // no-op
    }

    IRegimeProvider copy();
}
