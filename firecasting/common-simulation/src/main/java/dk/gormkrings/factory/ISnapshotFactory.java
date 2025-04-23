package dk.gormkrings.factory;

import dk.gormkrings.data.ILiveData;
import dk.gormkrings.result.ISnapshot;

public interface ISnapshotFactory {
    ISnapshot snapshot(ILiveData liveData);

}
