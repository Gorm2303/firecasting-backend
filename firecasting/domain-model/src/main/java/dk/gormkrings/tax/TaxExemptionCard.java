package dk.gormkrings.tax;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Getter
@Component
@Scope("prototype")
public class TaxExemptionCard implements ITaxRule {
    private float currentExemption = 0;
    @Value("${tax.exemption-card.limit}")
    private float limit = 0;
    @Value("${tax.exemption-card.increase}")
    private float yearlyLimitIncrease = 0;

    public TaxExemptionCard() {
    }

    @Override
    public double calculateTax(double amount) {
        if (currentExemption + amount < limit) {
            currentExemption += (float) amount;
            return 0;
        } else if (limit - currentExemption > 0) {
            float exemption = limit - currentExemption;
            currentExemption += exemption;
            return amount - exemption;
        } else {
            return amount;
        }

    }

    @Override
    public void yearlyUpdate() {
        this.limit += this.yearlyLimitIncrease;
        this.currentExemption = 0;
    }

    @Override
    public String toString() {
        return "{" + currentExemption + "/" + limit + "}";
    }

    public TaxExemptionCard copy() {
        TaxExemptionCard copy = new TaxExemptionCard();
        copy.limit = this.limit;
        copy.yearlyLimitIncrease = this.yearlyLimitIncrease;
        return copy;
    }
}
