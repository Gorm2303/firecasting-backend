package dk.gormkrings.returns;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import dk.gormkrings.simulation.ReturnStep;

@Slf4j
@Component
@Setter
@Getter
public class SimpleDailyReturn implements IReturner {
    private float averagePercentage = 0.07F;

    private final ReturnStep returnStep;

    public SimpleDailyReturn(ReturnStep returnStep) {
        this.returnStep = (returnStep == null) ? ReturnStep.DAILY : returnStep;
        log.debug("Initializing SimpleDailyReturn: {} (step={})", averagePercentage, this.returnStep);
    }

    @Override
    public double calculateReturn(double amount) {
        // Interpret averagePercentage as an annual expected return rate and scale to the configured step.
        double dt = returnStep.toDt();
        double perStepRate = Math.pow(1.0 + averagePercentage, dt) - 1.0;
        return amount * perStepRate;
    }

    public SimpleDailyReturn copy() {
        SimpleDailyReturn copy = new SimpleDailyReturn(returnStep);
        copy.setAveragePercentage(averagePercentage);
        return copy;
    }
}
