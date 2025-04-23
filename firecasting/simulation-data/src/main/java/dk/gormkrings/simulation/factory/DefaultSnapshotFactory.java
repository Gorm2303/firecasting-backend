package dk.gormkrings.simulation.factory;

import dk.gormkrings.data.ILiveData;
import dk.gormkrings.factory.ISnapshotFactory;
import dk.gormkrings.result.ISnapshot;
import dk.gormkrings.simulation.result.Snapshot;
import org.springframework.stereotype.Component;

@Component
public class DefaultSnapshotFactory implements ISnapshotFactory {

    @Override
    public ISnapshot snapshot(ILiveData liveData) {
        return new Snapshot(liveData);
    }
}
