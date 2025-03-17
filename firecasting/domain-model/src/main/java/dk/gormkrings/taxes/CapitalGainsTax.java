package dk.gormkrings.taxes;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CapitalGainsTax implements TaxRule {
    private float taxRate;
    private StockExemptionTax stockExemptionTax;
    private TaxExemptionCard taxExemptionCard;

    @Override
    public double calculateTax() {
        return 0;
    }
}
