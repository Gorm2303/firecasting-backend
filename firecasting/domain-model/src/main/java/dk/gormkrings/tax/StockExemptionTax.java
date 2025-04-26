package dk.gormkrings.tax;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Getter
@Setter
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

    @PostConstruct
    public void init() {
        setCurrentExemption(0);
        setTaxRate(taxRate);
        setLimit(limit);
        setYearlyLimitIncrease(yearlyLimitIncrease);
    }

    @Override
    public double calculateTax(double amount) {
        if (currentExemption >= limit) {
            return 0;
        }

        double exemptLeft = limit - currentExemption;
        if (amount <= exemptLeft) {
            currentExemption += (float) amount;
            return amount * taxRate / 100.0;
        } else {
            currentExemption = limit;
            return exemptLeft * taxRate / 100.0;
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