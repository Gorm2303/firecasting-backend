package dk.gormkrings.simulation.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DateArithmeticTest {
    @Test
    public void testPlusDays() {
        Date baseDate = new Date(1900, 1, 1);

        Date nextDay = baseDate.plusDays(1);
        assertEquals(1900, nextDay.getYear(), "Year should remain 1900 when adding one day");
        assertEquals(1, nextDay.getMonth(), "Month should remain January when adding one day");
        assertEquals(2, nextDay.getDayOfMonth(), "Day should be 2 after adding one day");

        Date previousDay = baseDate.plusDays(-1);
        assertEquals(1899, previousDay.getYear(), "Year should be 1899 when subtracting one day from 1900-01-01");
        assertEquals(12, previousDay.getMonth(), "Month should be December when subtracting one day from 1900-01-01");
        assertEquals(31, previousDay.getDayOfMonth(), "Day should be 31 when subtracting one day from 1900-01-01");
    }

    @Test
    public void testPlusMonthsSameYear() {
        Date baseDate = new Date(2021, 3, 15);
        Date resultDate = baseDate.plusMonths(2);
        assertEquals(2021, resultDate.getYear(), "Year should remain 2021 when adding 2 months to March 2021");
        assertEquals(5, resultDate.getMonth(), "Month should be May (5) when adding 2 months to March (3)");
        assertEquals(15, resultDate.getDayOfMonth(), "Day should remain 15 when the target month has enough days");
    }

    @Test
    public void testPlusMonthsCrossYear() {
        Date baseDate = new Date(2021, 11, 15);
        Date resultDate = baseDate.plusMonths(2);
        assertEquals(2022, resultDate.getYear(), "Year should roll over to 2022 when adding 2 months to November 2021");
        assertEquals(1, resultDate.getMonth(), "Month should be January (1) in the new year");
        assertEquals(15, resultDate.getDayOfMonth(), "Day should remain 15 across the year boundary");
    }

    @Test
    public void testPlusMonthsAdjustDayNonLeapYear() {
        Date baseDate = new Date(2021, 1, 31);
        Date resultDate = baseDate.plusMonths(1);
        assertEquals(2021, resultDate.getYear(), "Year should remain 2021 when adding 1 month to January 2021");
        assertEquals(2, resultDate.getMonth(), "Month should be February (2) when adding 1 month to January (1)");
        assertEquals(28, resultDate.getDayOfMonth(), "Day should be adjusted to 28 because February 2021 has only 28 days");
    }

    @Test
    public void testPlusMonthsAdjustDayLeapYear() {
        Date baseDate = new Date(2020, 1, 31);
        Date resultDate = baseDate.plusMonths(1);
        assertEquals(2020, resultDate.getYear(), "Year should remain 2020 when adding 1 month to January 2020");
        assertEquals(2, resultDate.getMonth(), "Month should be February (2) when adding 1 month to January (1)");
        assertEquals(29, resultDate.getDayOfMonth(), "Day should be adjusted to 29 because February 2020 has 29 days (leap year)");
    }
}
