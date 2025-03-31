package dk.gormkrings.simulation.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DateConstructorTest {
    @Test
    public void testEpochDayConstructor() {
        Date date = new Date(0);
        assertEquals(1900, date.getYear(), "Year should be 1900 for epochDay 0");
        assertEquals(1, date.getMonth(), "Month should be January for epochDay 0");
        assertEquals(1, date.getDayOfMonth(), "Day should be 1 for epochDay 0");
    }

    @Test
    public void testYearMonthDayConstructor() {
        Date date1 = new Date(1900, 1, 1);
        assertEquals(0, date1.getEpochDay(), "1900-01-01 should correspond to epochDay 0");
        assertEquals(1900, date1.getYear());
        assertEquals(1, date1.getMonth());
        assertEquals(1, date1.getDayOfMonth());

        Date date2 = new Date(2000, 2, 29);
        assertEquals(2000, date2.getYear(), "Year should be 2000 for 2000-02-29");
        assertEquals(2, date2.getMonth(), "Month should be February for 2000-02-29");
        assertEquals(29, date2.getDayOfMonth(), "Day should be 29 for 2000-02-29");

        Date reconstructedDate2 = new Date(date2.getEpochDay());
        assertEquals(date2.getYear(), reconstructedDate2.getYear());
        assertEquals(date2.getMonth(), reconstructedDate2.getMonth());
        assertEquals(date2.getDayOfMonth(), reconstructedDate2.getDayOfMonth());

        Date date3 = new Date(2021, 1, 31);
        assertEquals(2021, date3.getYear());
        assertEquals(1, date3.getMonth());
        assertEquals(31, date3.getDayOfMonth());

        Date date4 = new Date(2020, 3, 1);
        assertEquals(2020, date4.getYear());
        assertEquals(3, date4.getMonth());
        assertEquals(1, date4.getDayOfMonth());
    }

}
