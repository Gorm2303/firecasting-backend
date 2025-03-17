package dk.gormkrings.plans;

import dk.gormkrings.segments.Segment;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class FinancialPlan implements Plan {
    private List<Segment> accounts;

    public FinancialPlan() {
        this.accounts = new ArrayList<>();
    }

    public void addSegment(Segment segment) {
        this.accounts.add(segment);
    }

    public void removeSegment(Segment segment) {
        this.accounts.remove(segment);
    }

    @Override
    public void execute(List<Segment> segments) {

    }
}
