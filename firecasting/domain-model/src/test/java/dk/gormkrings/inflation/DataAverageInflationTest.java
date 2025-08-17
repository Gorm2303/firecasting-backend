package dk.gormkrings.inflation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataAverageInflationTest {

    @Test
    void testCalculateInflationWithValidData() {
        DataAverageInflation inflation = new DataAverageInflation("/dk/gormkrings/inflation/inflation-test.csv");
        double average = inflation.calculateInflation();
        assertEquals(3.3333, average, 0.0001, "Average inflation should be approximately 3.3333");
    }

    @Test
    void testCalculateInflationWithResourceNotFound() {
        DataAverageInflation inflation = new DataAverageInflation("/nonexistent.csv");
        double average = inflation.calculateInflation();
        assertEquals(0.0, average, "When resource is not found, average inflation should be 0");
    }

    @Test
    void testCopyCreatesEquivalentInstance() {
        DataAverageInflation inflation = new DataAverageInflation("/src/test/inflation/inflation-test.csv");
        IInflation copy = inflation.copy();
        assertEquals(inflation.calculateInflation(), copy.calculateInflation(), 0.0001,
                "Copy should have the same average inflation percentage");
        assertNotSame(inflation, copy, "Copy should be a different instance");
    }
}
