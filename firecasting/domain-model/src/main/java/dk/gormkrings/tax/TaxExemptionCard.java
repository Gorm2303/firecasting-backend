package dk.gormkrings.tax;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Getter
@Component
@Scope("prototype")
public class TaxExemptionCard {
    private float currentExemption = 0;
    @Value("${tax.exemption-card.limit}")
    private float limit = 0;
    @Value("${tax.exemption-card.increase}")
    private float yearlyLimitIncrease = 0;

    public TaxExemptionCard() {
    }

    public void yearlyLimitIncrease() {
        limit += yearlyLimitIncrease;
    }

    public void yearlyReset() {
        currentExemption = 0;
    }

    @Override
    public String toString() {
        return "{" + limit + "}";
    }

    public TaxExemptionCard copy() {
        TaxExemptionCard copy = new TaxExemptionCard();
        copy.limit = this.limit;
        copy.yearlyLimitIncrease = this.yearlyLimitIncrease;
        return copy;
    }
}
