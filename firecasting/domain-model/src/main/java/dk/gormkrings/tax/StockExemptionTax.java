package dk.gormkrings.tax;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Getter
@Component
@Scope("prototype")
public class StockExemptionTax {
    private float currentExemption = 0;
    @Value("${tax.stock-exemption.tax-rate}")
    private float taxRate;
    @Value("${tax.stock-exemption.limit}")
    private float limit = 0;
    @Value("${tax.stock-exemption.increase}")
    private float yearlyLimitIncrease = 0;

    public StockExemptionTax() {
    }

    public void yearlyIncrease() {
        this.limit += this.yearlyLimitIncrease;
    }

    public void yearlyReset() {
        this.currentExemption = 0;
    }

    @Override
    public String toString() {
        return "{" + taxRate + " " + limit + "}";
    }

    public StockExemptionTax copy() {
        StockExemptionTax copy = new StockExemptionTax();
        copy.limit = this.limit;
        copy.taxRate = this.taxRate;
        copy.yearlyLimitIncrease = this.yearlyLimitIncrease;
        return copy;
    }
}
