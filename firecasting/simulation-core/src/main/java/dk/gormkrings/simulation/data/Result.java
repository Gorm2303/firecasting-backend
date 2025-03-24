package dk.gormkrings.simulation.data;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

@Slf4j
@Getter
@Component
public class Result {
    private final List<Snapshot> snapshots;

    public Result() {
        this.snapshots = new LinkedList<>();
    }

    public void addSnapshot(Snapshot snapshot) {
        snapshots.add(snapshot);
    }

    public void addResult(Result result) {
        this.snapshots.addAll(result.getSnapshots());
    }

    public void print() {
        snapshots.forEach((e) -> log.debug(String.valueOf(e)));
    }

}
