package dk.gormkrings.inflation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataAverageInflationTest {

    @Test
    void testCalculatePercentageWithValidData() {
        DataAverageInflation inflation = new DataAverageInflation("/dk/gormkrings/inflation/inflation-test.csv");
        double average = inflation.calculatePercentage();
        assertEquals(3.3333, average, 0.0001, "Average inflation should be approximately 3.3333");
    }

    @Test
    void testCalculatePercentageWithResourceNotFound() {
        DataAverageInflation inflation = new DataAverageInflation("/nonexistent.csv");
        double average = inflation.calculatePercentage();
        assertEquals(0.0, average, "When resource is not found, average inflation should be 0");
    }

    @Test
    void testCopyCreatesEquivalentInstance() {
        DataAverageInflation inflation = new DataAverageInflation("/src/test/inflation/inflation-test.csv");
        IInflation copy = inflation.copy();
        assertEquals(inflation.calculatePercentage(), copy.calculatePercentage(), 0.0001,
                "Copy should have the same average inflation percentage");
        assertNotSame(inflation, copy, "Copy should be a different instance");
    }
}
