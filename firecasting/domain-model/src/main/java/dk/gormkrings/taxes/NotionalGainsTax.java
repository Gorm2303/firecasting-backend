package dk.gormkrings.taxes;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class NotionalGainsTax implements TaxRule {
    private final double taxRate;
    private double previousReturned = 0;
    private StockExemptionTax stockExemptionTax;
    private TaxExemptionCard taxExemptionCard;

    public NotionalGainsTax(double taxRate) {
        this(taxRate, null, null);
    }

    public NotionalGainsTax(double taxRate, StockExemptionTax stockExemptionTax, TaxExemptionCard taxExemptionCard) {
        this.taxRate = taxRate;
        this.stockExemptionTax = stockExemptionTax;
        this.taxExemptionCard = taxExemptionCard;
        log.debug("Notional Gains Tax Rule Created: {}", taxRate);
    }

    @Override
    public double calculateTax(double amount) {
        return amount * taxRate / 100;
    }

    @Override
    public NotionalGainsTax copy() {
        return new NotionalGainsTax(
                this.taxRate,
                this.stockExemptionTax,
                this.taxExemptionCard
        );
    }
}
