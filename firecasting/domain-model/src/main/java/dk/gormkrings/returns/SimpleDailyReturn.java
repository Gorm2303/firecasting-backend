package dk.gormkrings.returns;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SimpleDailyReturn implements IReturner {
    private final float averagePercentage;

    public SimpleDailyReturn(@Value("${simpleYearlyReturn.averagePercentage:0.07}") float averagePercentage) {
        this.averagePercentage = averagePercentage;
        log.debug("Initializing SimpleDailyReturn: {}", averagePercentage);
    }

    @Override
    public double calculateReturn(double amount) {
        return (amount * averagePercentage) / 252;
    }

    public SimpleDailyReturn copy() {
        return new SimpleDailyReturn(averagePercentage);
    }
}
