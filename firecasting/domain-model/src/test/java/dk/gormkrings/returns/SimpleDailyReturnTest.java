package dk.gormkrings.returns;

import dk.gormkrings.simulation.ReturnStep;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimpleDailyReturnTest {

    @Test
    void testCalculateReturn() {
        SimpleDailyReturn dailyReturn = new SimpleDailyReturn(ReturnStep.DAILY);
        dailyReturn.setAveragePercentage(0.07F);
        double amount = 1200;
        double expectedPerStep = Math.pow(1.0 + 0.07, ReturnStep.DAILY.toDt()) - 1.0;
        double expected = amount * expectedPerStep;
        double result = dailyReturn.calculateReturn(amount);
        assertEquals(expected, result, 1e-8);
    }

    @Test
    void testCopy() {
        SimpleDailyReturn original = new SimpleDailyReturn(ReturnStep.DAILY);
        original.setAveragePercentage(0.07F);
        SimpleDailyReturn copy = original.copy();
        double amount = 1000;
        assertEquals(original.calculateReturn(amount), copy.calculateReturn(amount), 0.0001,
                "The copy should compute the same return as the original");
        assertNotSame(original, copy, "The copy should be a different instance");
    }
}
