package dk.gormkrings.data;

public interface ILive {
    long getSessionDuration();
    void incrementTime();
    void incrementTime(long amount);
    boolean isLive(long duration);
    ILive copy();
    void resetSession();
}
