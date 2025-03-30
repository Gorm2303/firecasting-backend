package dk.gormkrings.plan;

import dk.gormkrings.segment.ISegment;

import java.util.List;

public interface IPlan {
    void execute(List<ISegment> segments);
}
