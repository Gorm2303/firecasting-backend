package dk.gormkrings.tax;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class NotionalGainsTax implements ITaxRule {
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
    public void yearlyReset() {
        stockExemptionTax.yearlyReset();
        taxExemptionCard.yearlyReset();
    }

    @Override
    public NotionalGainsTax copy() {
        NotionalGainsTax notionalGainsTax = new NotionalGainsTax(
                this.taxRate,
                this.stockExemptionTax,
                this.taxExemptionCard
        );
        notionalGainsTax.setPreviousReturned(this.previousReturned);
        return notionalGainsTax;
    }
}
