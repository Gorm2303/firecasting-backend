package dk.gormkrings.inflation;

import org.springframework.context.event.SmartApplicationListener;

public interface Inflation extends SmartApplicationListener {
    double calculatePercentage();
    Inflation copy();
}
