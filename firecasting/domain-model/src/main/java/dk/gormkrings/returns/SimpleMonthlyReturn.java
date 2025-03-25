package dk.gormkrings.returns;


import dk.gormkrings.util.Util;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimpleMonthlyReturn implements Return {
    private final float averagePercentage;

    public SimpleMonthlyReturn(float averagePercentage) {
        this.averagePercentage = averagePercentage;
        log.debug("Initializing SimpleMonthlyReturn: {}", Util.formatNumber(averagePercentage));
    }

    @Override
    public double calculateReturn(double amount) {
        return (amount * averagePercentage / 100) / 12;
    }

    public SimpleMonthlyReturn copy() {
        return new SimpleMonthlyReturn(averagePercentage);
    }
}
