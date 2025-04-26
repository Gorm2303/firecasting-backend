package dk.gormkrings.result;

import dk.gormkrings.data.IImmutableData;

import java.util.List;

public interface IRunResult extends IImmutableData {
    void addSnapshot(ISnapshot snapshot);
    List<ISnapshot> getSnapshots();
    void addResult(IRunResult result);
    String toString();
}
