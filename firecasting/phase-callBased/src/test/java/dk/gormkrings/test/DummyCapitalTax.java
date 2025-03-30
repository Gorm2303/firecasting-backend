package dk.gormkrings.test;

import dk.gormkrings.tax.ITaxRule;
import dk.gormkrings.tax.StockExemptionTax;
import dk.gormkrings.tax.TaxExemptionCard;

public class DummyCapitalTax implements ITaxRule {
        private final double taxRate;
        private StockExemptionTax stockExemptionTax;
        private TaxExemptionCard taxExemptionCard;

        public DummyCapitalTax(double taxRate) {
            this(taxRate, null, null);
        }

        public DummyCapitalTax(double taxRate, StockExemptionTax stockExemptionTax, TaxExemptionCard taxExemptionCard) {
            this.taxRate = taxRate;
            this.stockExemptionTax = stockExemptionTax;
            this.taxExemptionCard = taxExemptionCard;
        }

        @Override
        public double calculateTax(double amount) {
            return amount * taxRate / 100;
        }

        @Override
        public ITaxRule copy() {
            return null;
        }
}
