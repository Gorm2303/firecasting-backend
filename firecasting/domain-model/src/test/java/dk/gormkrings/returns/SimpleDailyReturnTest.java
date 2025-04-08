package dk.gormkrings.returns;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimpleDailyReturnTest {

    @Test
    void testCalculateReturn() {
        SimpleDailyReturn monthlyReturn = new SimpleDailyReturn();
        double amount = 1200;
        double expected = amount * 0.07 / 252;
        double result = monthlyReturn.calculateReturn(amount);
        assertEquals(expected, result, 0.0001, "The calculated monthly return should be 12");
    }

    @Test
    void testCopy() {
        SimpleDailyReturn original = new SimpleDailyReturn();
        SimpleDailyReturn copy = original.copy();
        double amount = 1000;
        assertEquals(original.calculateReturn(amount), copy.calculateReturn(amount), 0.0001,
                "The copy should compute the same return as the original");
        assertNotSame(original, copy, "The copy should be a different instance");
    }
}
