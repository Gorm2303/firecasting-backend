package dk.gormkrings.plans;

import dk.gormkrings.segments.Segment;

import java.util.List;

public interface Plan {
    void execute(List<Segment> segments);
}
