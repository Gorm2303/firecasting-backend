package dk.gormkrings.data;

public interface Live {
    long getSessionDuration();
    void incrementTime();
    void incrementTime(long amount);
    boolean isLive(long duration);
    Live copy();
    void resetSession();
}
