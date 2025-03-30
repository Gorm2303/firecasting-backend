package dk.gormkrings.plan;

import dk.gormkrings.segment.ISegment;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class FinancialPlan implements IPlan {
    private List<ISegment> accounts;

    public FinancialPlan() {
        this.accounts = new ArrayList<>();
    }

    public void addSegment(ISegment segment) {
        this.accounts.add(segment);
    }

    public void removeSegment(ISegment segment) {
        this.accounts.remove(segment);
    }

    @Override
    public void execute(List<ISegment> segments) {

    }
}
