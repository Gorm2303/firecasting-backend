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
        CapitalGainsTax original = new CapitalGainsTax(taxRate);
        CapitalGainsTax copy = original.copy();

        assertEquals(original.getTaxRate(), copy.getTaxRate(),
                "Copied tax rate should equal original tax rate");
        assertNotSame(original, copy, "Copy should be a different instance");
    }
}
