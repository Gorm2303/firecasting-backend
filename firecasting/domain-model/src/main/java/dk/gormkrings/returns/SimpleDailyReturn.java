package dk.gormkrings.returns;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Setter
@Getter
@ConfigurationProperties(prefix = "returner.simple")
public class SimpleDailyReturn implements IReturner {
    private float averagePercentage;

    public SimpleDailyReturn() {
        log.debug("Initializing SimpleDailyReturn: {}", averagePercentage);
    }

    @Override
    public double calculateReturn(double amount) {
        return (amount * averagePercentage) / 252;
    }

    public SimpleDailyReturn copy() {
        SimpleDailyReturn copy = new SimpleDailyReturn();
        copy.setAveragePercentage(averagePercentage);
        return copy;
    }
}
