package dk.gormkrings.simulation.data;

import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

@Component
public class Result {
    private final List<Snapshot> snapshots;

    public Result() {
        this.snapshots = new LinkedList<>();
    }

    public void addSnapshot(Snapshot snapshot) {
        snapshots.add(snapshot);
    }

    public void print() {
        snapshots.forEach(System.out::println);
    }

}
