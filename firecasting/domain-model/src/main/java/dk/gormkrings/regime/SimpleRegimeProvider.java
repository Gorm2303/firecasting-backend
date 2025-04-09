package dk.gormkrings.regime;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@ConfigurationProperties(prefix = "regime")
public class SimpleRegimeProvider implements IRegimeProvider {

    @Setter
    private int currentRegime = 0;

    /**
     * Transition matrix can be configured from application.properties.
     * By default, it is set as:
     * state 0: 90% chance to remain in 0, 10% to switch to 1
     * state 1: 15% chance to switch to 0, 85% to remain in 1
     */
    @Setter
    @Getter
    private double[][] transitionMatrix = {
            {0.90, 0.10},
            {0.15, 0.85}
    };

    private final Random random = new Random();

    @Override
    public int getCurrentRegime() {
        double p = random.nextDouble();
        double cumulative = 0.0;
        int newRegime = currentRegime;
        for (int i = 0; i < transitionMatrix[currentRegime].length; i++) {
            cumulative += transitionMatrix[currentRegime][i];
            if (p < cumulative) {
                newRegime = i;
                break;
            }
        }
        currentRegime = newRegime;
        return currentRegime;
    }
}
