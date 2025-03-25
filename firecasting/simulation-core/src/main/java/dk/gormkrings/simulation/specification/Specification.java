package dk.gormkrings.simulation.specification;

import dk.gormkrings.data.LiveData;
import dk.gormkrings.inflation.Inflation;
import dk.gormkrings.returns.Return;
import dk.gormkrings.taxes.TaxRule;
import lombok.Getter;
import org.springframework.context.event.SmartApplicationListener;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Specification implements Spec {
    private final LiveData liveData;
    private final TaxRule taxRule;
    private final Return returner;
    private final Inflation inflation;
    private final List<SmartApplicationListener> listeners = new ArrayList<>();

    public Specification(LiveData livedata, TaxRule taxRule, Return returner, Inflation inflation) {
        this.liveData = livedata;
        this.taxRule = taxRule;
        this.returner = returner;
        this.inflation = inflation;
        addListener(inflation);
    }

    public void addListener(SmartApplicationListener listener) {
        listeners.add(listener);
    }

    @Override
    public Specification copy() {
        return new Specification(
                liveData.copy(),
                taxRule.copy(),
                returner.copy(),
                inflation.copy()
        );
    }

    @Override
    public List<SmartApplicationListener> getListeners() {
        return listeners;
    }
}
