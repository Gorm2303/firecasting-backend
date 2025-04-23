package dk.gormkrings.result;

import dk.gormkrings.data.IImmutableData;
import dk.gormkrings.data.ILiveData;

public interface ISnapshot extends IImmutableData {
    String toCsvRow();
    ILiveData getState();
}
