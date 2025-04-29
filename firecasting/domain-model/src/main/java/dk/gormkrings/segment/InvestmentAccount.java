package dk.gormkrings.segment;

import lombok.Getter;

public class InvestmentAccount implements ISegment {
    @Getter
    private String name = "InvestmentAccount";
    @Override
    public void process() {

    }
}
