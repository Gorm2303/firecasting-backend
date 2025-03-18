package dk.gormkrings.data;

public interface Live {
    int getDuration();
    int getCurrentTimeSpan();
    void incrementTime();
    boolean isLive();
}
