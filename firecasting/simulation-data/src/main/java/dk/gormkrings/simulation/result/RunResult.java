package dk.gormkrings.simulation.result;

import dk.gormkrings.result.IRunResult;
import dk.gormkrings.result.ISnapshot;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
@Component
@Scope("prototype")
public class RunResult implements IRunResult {
    private final List<ISnapshot> snapshots;

    public RunResult() {
        this.snapshots = new ArrayList<>();
    }

    @Override
    public void addSnapshot(ISnapshot snapshot) {
        snapshots.add(snapshot);
    }

    @Override
    public void addResult(IRunResult result) {
        this.snapshots.addAll(result.getSnapshots());
    }
}
