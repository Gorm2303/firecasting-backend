package dk.gormkrings.tax;

import lombok.Getter;
import lombok.Setter;

@Getter
public class StockExemptionTax {
    private final float taxRate;
    private float limit;
    @Setter
    private float yearlyLimitIncrement = 0;

    public StockExemptionTax(float taxRate, float limit) {
        this.taxRate = taxRate;
        this.limit = limit;
    }

    public void yearlyIncrement() {
        this.limit += this.yearlyLimitIncrement;
    }
}
