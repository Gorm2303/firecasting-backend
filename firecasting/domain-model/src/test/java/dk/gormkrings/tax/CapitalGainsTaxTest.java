package dk.gormkrings.tax;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CapitalGainsTaxTest {

    @Test
    void testCalculateTaxWithoutExemptions() {
        double taxRate = 42.0;
        CapitalGainsTax taxRule = new CapitalGainsTax(taxRate);
        double amount = 1000.0;
        double expectedTax = (amount * taxRate) / 100;
        assertEquals(expectedTax, taxRule.calculateTax(amount), 0.0001,
                "Tax should be calculated as (amount * taxRate) / 100");
    }

    @Test
    void testCopy() {
        double taxRate = 42.0;
        CapitalGainsTax original = new CapitalGainsTax(taxRate, null, null);
        CapitalGainsTax copy = original.copy();

        assertEquals(original.getTaxRate(), copy.getTaxRate(),
                "Copied tax rate should equal original tax rate");
        assertEquals(original.getStockExemptionTax(), copy.getStockExemptionTax(),
                "Copied stock exemption tax should equal original");
        assertEquals(original.getTaxExemptionCard(), copy.getTaxExemptionCard(),
                "Copied tax exemption card should equal original");
        assertNotSame(original, copy, "Copy should be a different instance");
    }
}
