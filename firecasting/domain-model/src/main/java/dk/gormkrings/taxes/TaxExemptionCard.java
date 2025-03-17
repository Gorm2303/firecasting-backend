package dk.gormkrings.taxes;

import lombok.Getter;
import lombok.Setter;

@Getter
public class TaxExemptionCard {
    private float exemption;
    @Setter
    private float yearlyIncrease = 0;

    public TaxExemptionCard(float exemption) {
        this.exemption = exemption;
    }

    public void yearlyIncrement() {
        exemption += yearlyIncrease;
    }
}
