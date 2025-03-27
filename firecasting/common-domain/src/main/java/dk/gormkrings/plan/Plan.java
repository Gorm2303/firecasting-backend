package dk.gormkrings.plan;

import dk.gormkrings.segment.Segment;

import java.util.List;

public interface Plan {
    void execute(List<Segment> segments);
}
