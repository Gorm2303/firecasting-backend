package dk.gormkrings.simulation.specification;

import org.springframework.context.event.SmartApplicationListener;

import java.util.List;

public interface Spec {
    Spec copy();
    List<SmartApplicationListener> getListeners();
}
