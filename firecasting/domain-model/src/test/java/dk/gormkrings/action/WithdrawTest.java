package dk.gormkrings.action;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WithdrawTest {

    @Test
    void testConstructorRejectsBothNegative() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Withdraw(-10000, 5000, 0,0);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            new Withdraw(10000, -5000,0,0);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            new Withdraw(-10000, -5000,0,0);
        });
    }

    @Test
    void testGetMonthlyAmountDirect() {
        Withdraw withdraw = new Withdraw(500, 0,0,0);
        double capital = 100000;
        double monthly = withdraw.getMonthlyAmount(capital);
        assertEquals(500, monthly, "Should return the direct monthly amount when > 0");
    }

    @Test
    void testGetMonthlyAmountFromPercent() {
        Withdraw withdraw = new Withdraw(0, 0.12,0,0);
        double capital = 120000;
        double monthly = withdraw.getMonthlyAmount(capital);
        assertEquals(1200, monthly, "Should calculate monthly amount based on percent when monthlyAmount is 0");
    }

    @Test
    void copyMethodTest() {
        double monthlyAmount = 500.0;
        double monthlyPercent = 0.04;
        double capital = 10000.0;

        Withdraw original = new Withdraw(monthlyAmount, monthlyPercent,0,0);

        Withdraw copy = original.copy();

        assertNotSame(original, copy, "The copied instance should be a different object than the original");
        assertEquals(original.getMonthlyAmount(capital), copy.getMonthlyAmount(capital),
                "The monthly amount returned should be the same for the copy and the original");
        original.setMonthlyAmount(800.0);
        assertNotEquals(original.getMonthlyAmount(capital), copy.getMonthlyAmount(capital),
                "The copy should remain unchanged after modifying the original");
    }

    @Test
    void setterAndGetterTest() {
        double capital = 10000.0;

        Withdraw withdraw = new Withdraw(500.0, 0.06,0,0);

        double newMonthlyAmount = 0.0;
        double newMonthlyPercent = 0.12;
        withdraw.setMonthlyAmount(newMonthlyAmount);
        withdraw.setYearlyPercentage(newMonthlyPercent);

        double expected = (newMonthlyPercent * capital) / 12;
        assertEquals(expected, withdraw.getMonthlyAmount(capital),
                "After setting new values, getMonthlyAmount should compute based on monthlyPercent when monthlyAmount is not positive");

        withdraw.setMonthlyAmount(700.0);

        assertEquals(700.0, withdraw.getMonthlyAmount(capital),
                "When monthlyAmount is positive, getMonthlyAmount should return it directly");

        assertThrows(IllegalArgumentException.class, () -> {
            withdraw.setMonthlyAmount(-700.0);
        });
        assertNotEquals(-200.0, withdraw.getMonthlyAmount(capital), "The setter should not be able to set negative value");
    }
}
