package dk.gormkrings.data;

public interface Live {
    long getSessionDuration();
    void incrementTime();
    boolean isLive(long duration);
    Live copy();
    void resetSession();
}
