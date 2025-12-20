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

        // TaxExemptionCard with a yearly limit of €2 000
        TaxExemptionCard card = new TaxExemptionCard();
        card.setLimit(2_000);
        card.setCurrentExemption(0);

        IWithdrawPhase phase = WithdrawPhaseTestHelper
            .makePhaseWithRules(startingCapital, deposited, withdrawal, new CapitalGainsTax(42), card);

        // WHEN (estimate only)
        double rate = phase.estimateCapitalTaxRate(withdrawal, new CapitalGainsTax(42));

        // THEN
        // 1) All of the €2 000 exemption should apply first:
        //    taxableWithdrawal = (withdrawal * gainPct) - 2 000
        double gainPct = 1 - (deposited / startingCapital);
        double expectedTaxable = withdrawal * gainPct - 2_000;
        expectedTaxable = Math.max(0, expectedTaxable);

        // estimateCapitalTaxRate must not mutate exemption state
        assertEquals(0f, card.getCurrentExemption(), 1e-6, "estimate should not mutate the exemption card");

        // And that the next‐in‐line CapitalGainsTax (if added) would see the right base.
        assertEquals(expectedTaxable / withdrawal, rate, 1e-6);

        // WHEN (apply tax for real)
        phase.addCapitalTax();

        // THEN: exemption card consumed and tax is still zero
        assertEquals(2_000f, card.getCurrentExemption(), 1e-6, "Card should use its full exemption");
        assertEquals(0.0, phase.getLiveData().getCurrentTax(), 1e-6);
    }

    @Test
    void stockExemptionThenCapitalTax() {
        double startingCapital = 200000;
        double deposited       = 150000;
        double withdrawal      =  40000;

        // Suppose the first €5 000 of gains are in a lower/zero stock-tax band,
        // then a 25% capital gains tax applies on the remainder.
        StockExemptionTax stockRule = new StockExemptionTax();
        stockRule.setTaxRate(0);
        stockRule.setLimit(5_000);
        stockRule.setYearlyLimitIncrease(0);

        CapitalGainsTax capitalRule = new CapitalGainsTax(25);

        IWithdrawPhase phase = WithdrawPhaseTestHelper
            .makePhaseWithRules(startingCapital, deposited, withdrawal, capitalRule, stockRule);

        phase.addCapitalTax();   // actually apply addTax()

        // The taxable part is (withdrawal * gainPct) – 5 000,
        // and tax = 25 % of that remainder.
        double gainPct = 1 - (deposited / startingCapital);
        double rawTaxable = withdrawal * gainPct;
        double remainder = Math.max(0, rawTaxable - 5_000);
        double expectedTax = remainder * 0.25;

        assertEquals(expectedTax, phase.getLiveData().getCurrentTax(), 1e-6);
        // And stockRule’s used amount increased by exactly 5 000:
        assertEquals(5_000f, stockRule.getCurrentExemption(), 1e-6, "Stock band should consume 5 000");
    }
}
