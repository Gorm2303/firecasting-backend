package dk.gormkrings.returns;


import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimpleDailyReturn implements IReturner {
    private final float averagePercentage;

    public SimpleDailyReturn(float averagePercentage) {
        this.averagePercentage = averagePercentage;
        log.debug("Initializing SimpleMonthlyReturn: {}", averagePercentage);
    }

    @Override
    public double calculateReturn(double amount) {
        return (amount * averagePercentage / 100) / 252;
    }

    public SimpleDailyReturn copy() {
        return new SimpleDailyReturn(averagePercentage);
    }
}
