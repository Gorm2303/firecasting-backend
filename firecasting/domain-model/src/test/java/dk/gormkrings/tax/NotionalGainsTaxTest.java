package dk.gormkrings.tax;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NotionalGainsTaxTest {

    @Test
    void testCalculateTax() {
        double taxRate = 40.0;
        NotionalGainsTax taxRule = new NotionalGainsTax(taxRate);
        double amount = 2000.0;
        double expectedTax = (amount * taxRate) / 100;
        assertEquals(expectedTax, taxRule.calculateTax(amount), 0.0001,
                "Tax should be calculated as (amount * taxRate) / 100");
    }

    @Test
    void testCopyCreatesEquivalentInstance() {
        double taxRate = 40.0;
        NotionalGainsTax original = new NotionalGainsTax(taxRate);
        original.setPreviousReturned(500);

        NotionalGainsTax copy = original.copy();

        assertEquals(original.getTaxRate(), copy.getTaxRate(), 0.0001, "Tax rates should match");
        assertEquals(original.getPreviousReturned(), copy.getPreviousReturned(), 0.0001, "Previous returned values should match");
        assertEquals(original.getStockExemptionTax(), copy.getStockExemptionTax(), "Stock exemption tax should match");
        assertEquals(original.getTaxExemptionCard(), copy.getTaxExemptionCard(), "Tax exemption card should match");
        assertNotSame(original, copy, "Copy should be a distinct instance");
    }
}
