package dk.gormkrings.tax;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Getter
@Component
@Scope("prototype")
public class StockExemptionTax implements ITaxRule {
    private float currentExemption = 0;
    @Value("${tax.stock-exemption.tax-rate}")
    private float taxRate;
    @Value("${tax.stock-exemption.limit}")
    private float limit = 0;
    @Value("${tax.stock-exemption.increase}")
    private float yearlyLimitIncrease = 0;

    public StockExemptionTax() {
    }

    @Override
    public double calculateTax(double amount) {
        if (currentExemption + amount < limit) {
            currentExemption += (float) amount;
            return amount * taxRate / 100;
        } else if (limit - currentExemption > 0) {
            float exemption = limit - currentExemption;
            currentExemption += exemption;
            return exemption * taxRate / 100;
        } else {
            return 0;
        }
    }

    @Override
    public void yearlyUpdate() {
        this.limit += this.yearlyLimitIncrease;
        this.currentExemption = 0;
    }

    @Override
    public String toString() {
        return "{" + taxRate + " " + currentExemption + "/" + limit + "}";
    }

    public StockExemptionTax copy() {
        StockExemptionTax copy = new StockExemptionTax();
        copy.limit = this.limit;
        copy.taxRate = this.taxRate;
        copy.yearlyLimitIncrease = this.yearlyLimitIncrease;
        return copy;
    }
}
