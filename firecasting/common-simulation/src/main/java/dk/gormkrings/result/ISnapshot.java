package dk.gormkrings.result;

import dk.gormkrings.data.IImmutableData;

public interface ISnapshot extends IImmutableData {
    String toCsvRow();
}
