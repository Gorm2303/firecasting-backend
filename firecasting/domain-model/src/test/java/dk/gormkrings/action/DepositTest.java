package dk.gormkrings.action;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DepositTest {

    @Test
    void validConstructionTest() {
        double initial = 1000.0;
        double monthly = 100.0;

        Deposit deposit = new Deposit(initial, monthly,0);

        assertEquals(initial, deposit.getInitial(), "Initial value should match the provided value");
        assertEquals(monthly, deposit.getMonthly(), "Monthly value should match the provided value");
    }

    @Test
    void testConstructorRejectsBothNegative() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Deposit(-10000, 5000,0);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            new Deposit(10000, -5000,0);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            new Deposit(-10000, -5000,0);
        });
    }

    @Test
    void copyMethodTest() {
        double initial = 1000.0;
        double monthly = 100.0;
        Deposit original = new Deposit(initial, monthly,0);

        Deposit copy = original.copy();

        assertNotSame(original, copy, "The copied instance should be a different object than the original");
        assertEquals(original.getInitial(), copy.getInitial(), "The initial values of the copy and original should match");
        assertEquals(original.getMonthly(), copy.getMonthly(), "The monthly values of the copy and original should match");
        original.setMonthly(500);
        assertNotEquals(original.getMonthly(), copy.getMonthly(), "The monthly value of the copy should not change when setting monthly of the original");
    }

    @Test
    void setterAndGetterTest() {
        double initial = 1000.0;
        double monthly = 100.0;
        Deposit deposit = new Deposit(initial, monthly,0);

        double newMonthly = 200.0;
        deposit.setMonthly(newMonthly);

        assertEquals(newMonthly, deposit.getMonthly(), "The getter should return the updated monthly value");

        assertThrows(IllegalArgumentException.class, () -> {
            deposit.setMonthly(-200.0);
        });
        assertNotEquals(-200.0, deposit.getMonthly(), "The setter should not be able to set negative value");
    }

}
