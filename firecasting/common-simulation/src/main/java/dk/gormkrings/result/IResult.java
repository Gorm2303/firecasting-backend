package dk.gormkrings.result;

import dk.gormkrings.data.IImmutableData;

import java.util.List;

public interface IResult extends IImmutableData {
    void addSnapshot(ISnapshot snapshot);
    List<ISnapshot> getSnapshots();
    void addResult(IResult result);
}
