package dk.gormkrings.data;

public interface Live {
    int getCurrentTimeSpan();
    void incrementTime();
    boolean isLive(long duration);
}
