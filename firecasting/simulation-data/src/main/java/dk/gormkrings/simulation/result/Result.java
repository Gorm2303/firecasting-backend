package dk.gormkrings.simulation.result;

import dk.gormkrings.result.IResult;
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
public class Result implements IResult {
    private final List<ISnapshot> snapshots;

    public Result() {
        this.snapshots = new ArrayList<>();
    }

    @Override
    public void addSnapshot(ISnapshot snapshot) {
        snapshots.add(snapshot);
    }

    @Override
    public void addResult(IResult result) {
        this.snapshots.addAll(result.getSnapshots());
    }
}
