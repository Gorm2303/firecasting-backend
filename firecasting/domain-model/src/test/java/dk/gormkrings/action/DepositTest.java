package dk.gormkrings.action;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DepositTest {

    @Test
    void testConstructorInitialValues() {
        Deposit deposit = new Deposit(10000, 5000);
        assertEquals(10000, deposit.getInitial(), "Initial value should be 10000");
        assertEquals(5000, deposit.getMonthly(), "Monthly value should be 5000");
        assertEquals(0, deposit.getMonthlyIncrease(), "Monthly increase should be 0 initially");
    }

    @Test
    void testIncreaseMonthlyAmountTakesPrecedence() {
        Deposit deposit = new Deposit(10000, 5000);
        deposit.increaseMonthly(200);
        deposit.setIncreaseMonthlyPercentage(0.1);
        assertEquals(200, deposit.getMonthlyIncrease(), "IncreaseMonthlyAmount takes precedence over percentage");
    }

    @Test
    void testIncreaseMonthlyPercentageUsedWhenAmountIsZero() {
        Deposit deposit = new Deposit(10000, 5000);
        deposit.setIncreaseMonthlyPercentage(0.1);
        assertEquals(500, deposit.getMonthlyIncrease(), "Monthly increase should be computed as monthly * percentage");
    }

    @Test
    void testCopyCreatesEquivalentInstance() {
        Deposit deposit = new Deposit(10000, 5000);
        deposit.increaseMonthly(150);
        deposit.setIncreaseMonthlyPercentage(0.05);

        Deposit copy = deposit.copy();
        assertEquals(deposit.getInitial(), copy.getInitial(), "Initial values should be equal");
        assertEquals(deposit.getMonthly(), copy.getMonthly(), "Monthly values should be equal");
        assertEquals(deposit.getMonthlyIncrease(), copy.getMonthlyIncrease(), "Monthly increase should be equal");
        assertNotSame(deposit, copy, "Copy should be a different instance");
    }
}
