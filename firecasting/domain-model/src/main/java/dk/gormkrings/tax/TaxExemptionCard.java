package dk.gormkrings.tax;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Getter
@Component
@Scope("prototype")
public class TaxExemptionCard implements ITaxRule {
    @Setter
    private float currentExemption = 0;
    @Value("${tax.exemption-card.limit}")
    private float limit = 0;
    @Value("${tax.exemption-card.increase}")
    private float yearlyLimitIncrease = 0;

    public TaxExemptionCard() {
    }

    @Override
    public double calculateTax(double amount) {
        if (currentExemption >= limit) {
            return 0;
        }

        double exemptLeft = limit - currentExemption;
        if (amount <= exemptLeft) {
            currentExemption += (float) amount;
            return amount;
        } else {
            currentExemption += (float) exemptLeft;
            return exemptLeft;
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
