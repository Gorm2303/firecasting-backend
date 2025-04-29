package dk.gormkrings.plan;

import dk.gormkrings.segment.ISegment;

import java.util.List;

public interface IPlan {
    List<ISegment> getSegments();
    void execute();
}
