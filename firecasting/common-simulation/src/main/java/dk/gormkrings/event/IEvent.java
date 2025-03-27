package dk.gormkrings.event;

public interface IEvent {
    int getEpochDay();
    EventType getType();
}
