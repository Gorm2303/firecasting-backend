package dk.gormkrings.phase.callBased;

import static org.junit.jupiter.api.Assertions.*;

import dk.gormkrings.phase.IWithdrawPhase;
import dk.gormkrings.tax.CapitalGainsTax;
import dk.gormkrings.tax.StockExemptionTax;
import dk.gormkrings.tax.TaxExemptionCard;
import org.junit.jupiter.api.Test;

class TaxExemptionCardTest {

    @Test
    void exemptionCardIsSubtractedCorrectly() {
        // GIVEN
        double startingCapital = 100_000;
        double deposited       =  80_000;
        double withdrawal      =  10_000;

        // TaxExemptionCard with, say, €2 000 exemption remaining
        TaxExemptionCard card = new TaxExemptionCard();

        // No other rules
        IWithdrawPhase phase = WithdrawPhaseTestHelper
                .makePhaseWithRules(startingCapital, deposited, withdrawal, card);

        // WHEN
        double rate = phase.estimateCapitalTaxRate(withdrawal, new CapitalGainsTax(42));

        // THEN
        // 1) All of the €2 000 exemption should apply first:
        //    taxableWithdrawal = (withdrawal * gainPct) - 2 000
        double gainPct = 1 - (deposited / startingCapital);
        double expectedTaxable = withdrawal * gainPct - 2_000;
        expectedTaxable = Math.max(0, expectedTaxable);

        // Check that the card’s internal ‘used’ amount is 2 000
        float used = 2_000f - card.getCurrentExemption();
        assertEquals(2_000f, used, 1e-6, "Card should use its full exemption");

        // And that the next‐in‐line CapitalGainsTax (if added) would see the right base.
        assertEquals(expectedTaxable / withdrawal, rate, 1e-6);
    }

    @Test
    void stockExemptionThenCapitalTax() {
        double startingCapital = 200000;
        double deposited       = 150000;
        double withdrawal      =  20000;

        // Suppose €5 000 stock exemption,
        // then a 25 % capital gains tax on the rest.
        StockExemptionTax stockRule = new StockExemptionTax();
        CapitalGainsTax capitalRule = new CapitalGainsTax(0.25f);

        IWithdrawPhase phase = WithdrawPhaseTestHelper
                .makePhaseWithRules(startingCapital, deposited, withdrawal, stockRule, capitalRule);

        phase.addCapitalTax();   // actually apply addTax()

        // The taxable part is (withdrawal * gainPct) – 5 000,
        // and tax = 25 % of that remainder.
        double gainPct = 1 - (deposited / startingCapital);
        double rawTaxable = withdrawal * gainPct - 5_000;
        rawTaxable = Math.max(0, rawTaxable);
        double expectedTax = rawTaxable * 0.25;

        assertEquals(expectedTax, phase.getLiveData().getCurrentTax(), 1e-6);
        // And stockRule’s currentExemption reduced by exactly 5 000:
        assertEquals(0f, stockRule.getCurrentExemption(), "Stock exemption should be zeroed out");
    }
}
