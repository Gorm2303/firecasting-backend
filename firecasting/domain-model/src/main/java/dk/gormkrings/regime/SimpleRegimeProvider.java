package dk.gormkrings.regime;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.SplittableRandom;

@Setter
@Component
@ConfigurationProperties(prefix = "regime")
public class SimpleRegimeProvider implements IRegimeProvider {

    private int currentRegime = 0;

    /**
     * Transition matrix can be configured from application.properties.
     * By default, it is set as:
     * state 0: 90% chance to remain in 0, 10% to switch to 1
     * state 1: 15% chance to switch to 0, 85% to remain in 1
     */
    @Getter
    private double[][] transitionMatrix = {
            {0.90, 0.10},
            {0.15, 0.85}
    };
    @Getter
    private SplittableRandom random = new SplittableRandom();

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

    @Override
    public IRegimeProvider copy() {
        SimpleRegimeProvider copy = new SimpleRegimeProvider();
        copy.currentRegime = currentRegime;
        copy.transitionMatrix = transitionMatrix;
        copy.random = random.split();
        return copy;
    }


}
