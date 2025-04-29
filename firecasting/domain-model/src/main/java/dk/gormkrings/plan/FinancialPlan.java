package dk.gormkrings.plan;

import dk.gormkrings.segment.ISegment;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class FinancialPlan implements IPlan {
    private List<ISegment> segments;

    public FinancialPlan() {
        this.segments = new ArrayList<>();
    }

    public void addSegment(ISegment segment) {
        this.segments.add(segment);
    }

    public void removeSegment(ISegment segment) {
        this.segments.remove(segment);
    }

    @Override
    public void execute() {
        for (ISegment segment : segments) {
            segment.process();
        }
    }
}
