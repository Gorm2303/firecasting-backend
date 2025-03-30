package dk.gormkrings.action;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WithdrawTest {

    @Test
    void testGetMonthlyAmountDirect() {
        Withdraw withdraw = new Withdraw(500, 0.1);
        double capital = 100000;
        double monthly = withdraw.getMonthlyAmount(capital);
        assertEquals(500, monthly, "Should return the direct monthly amount when > 0");
    }

    @Test
    void testGetMonthlyAmountFromPercent() {
        Withdraw withdraw = new Withdraw(0, 0.12);
        double capital = 120000;
        double monthly = withdraw.getMonthlyAmount(capital);
        assertEquals(1200, monthly, "Should calculate monthly amount based on percent when monthlyAmount is 0");
    }

    @Test
    void testCopyCreatesEquivalentInstance() {
        Withdraw original = new Withdraw(700, 0.08);
        Withdraw copy = original.copy();
        double capital = 100000;
        assertEquals(original.getMonthlyAmount(capital), copy.getMonthlyAmount(capital), "Copy should have the same monthly amount");
        assertNotSame(original, copy, "Copy should be a different instance");
    }
}
