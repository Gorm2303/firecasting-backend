package dk.gormkrings.tax;

import lombok.Getter;
import lombok.Setter;

/**
 * Optional overrides for tax exemptions.
 */
@Getter
@Setter
public class TaxExemptionConfig {
    private ExemptionCardConfig exemptionCard;
    private StockExemptionConfig stockExemption;

    @Getter
    @Setter
    public static class ExemptionCardConfig {
        private Float limit;
        private Float yearlyIncrease;
    }

    @Getter
    @Setter
    public static class StockExemptionConfig {
        private Float taxRate;
        private Float limit;
        private Float yearlyIncrease;
    }
}