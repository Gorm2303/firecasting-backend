package dk.gormkrings.tax;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
@Getter
public class CapitalGainsTax implements TaxRule {
    private final double taxRate;
    private StockExemptionTax stockExemptionTax;
    private TaxExemptionCard taxExemptionCard;

    public CapitalGainsTax(double taxRate) {
        this(taxRate, null, null);
    }

    public CapitalGainsTax(double taxRate, StockExemptionTax stockExemptionTax, TaxExemptionCard taxExemptionCard) {
        this.taxRate = taxRate;
        this.stockExemptionTax = stockExemptionTax;
        this.taxExemptionCard = taxExemptionCard;
        log.debug("Capital Gains Tax Rule Created: {}", taxRate);
    }

    @Override
    public double calculateTax(double amount) {
        return amount * taxRate / 100;
    }

    @Override
    public CapitalGainsTax copy() {
        return new CapitalGainsTax(
                this.taxRate,
                this.stockExemptionTax,
                this.taxExemptionCard
                );
    }
}
