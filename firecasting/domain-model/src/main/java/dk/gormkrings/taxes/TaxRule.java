package dk.gormkrings.taxes;

import org.springframework.context.event.SmartApplicationListener;

public interface TaxRule extends SmartApplicationListener {
    double calculateTax();
    TaxRule copy();
}
