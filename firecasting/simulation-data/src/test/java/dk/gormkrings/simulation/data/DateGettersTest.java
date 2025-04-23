package dk.gormkrings.simulation.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DateGettersTest {
    @Test
    public void testGettersForEpochDayZero() {
        Date date = new Date(1900, 1, 1);
        assertEquals(1900, date.getYear(), "Year should be 1900 for 1900-01-01");
        assertEquals(1, date.getMonth(), "Month should be January for 1900-01-01");
        assertEquals(1, date.getDayOfMonth(), "Day of month should be 1 for 1900-01-01");
        assertEquals(1, date.getDayOfYear(), "Day of year should be 1 for 1900-01-01");
    }

    @Test
    public void testGettersForNonLeapYear() {
        Date date = new Date(2019, 3, 1);
        assertEquals(2019, date.getYear(), "Year should be 2019 for 2019-03-01");
        assertEquals(3, date.getMonth(), "Month should be March for 2019-03-01");
        assertEquals(1, date.getDayOfMonth(), "Day of month should be 1 for 2019-03-01");
        assertEquals(60, date.getDayOfYear(), "Day of year should be 60 for 2019-03-01");
    }

    @Test
    public void testGettersForLeapYear() {
        Date date = new Date(2020, 3, 1);
        assertEquals(2020, date.getYear(), "Year should be 2020 for 2020-03-01");
        assertEquals(3, date.getMonth(), "Month should be March for 2020-03-01");
        assertEquals(1, date.getDayOfMonth(), "Day of month should be 1 for 2020-03-01");
        assertEquals(61, date.getDayOfYear(), "Day of year should be 61 for 2020-03-01");
    }
}
