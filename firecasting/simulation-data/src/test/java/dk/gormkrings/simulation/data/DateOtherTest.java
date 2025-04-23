package dk.gormkrings.simulation.data;

import dk.gormkrings.data.IDate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DateOtherTest {
    @Test
    public void testEqualsAndHashCode() {
        Date date1 = new Date(2021, 5, 20);
        Date date2 = new Date(date1.getEpochDay());
        assertEquals(date1, date2, "Dates with the same epochDay should be equal");
        assertEquals(date1.hashCode(), date2.hashCode(), "Equal dates should have the same hash code");
    }

    @Test
    public void testToStringFormat() {
        Date date = new Date(2021, 5, 20);
        String expected = "2021-05-20";
        assertEquals(expected, date.toString(), "toString should return the date in YYYY-MM-DD format");
    }

    @Test
    public void testDaysUntil() {
        Date baseDate = new Date(100);
        IDate mockDate = mock(IDate.class);
        when(mockDate.getEpochDay()).thenReturn(110);
        long daysDifference = baseDate.daysUntil(mockDate);
        assertEquals(10, daysDifference, "daysUntil should compute the difference in epoch days correctly.");
    }

    @Test
    public void testNegativeDays() {
        Date date = new Date(1900, 1, 1);
        Date previousDay = date.plusDays(-1);
        assertEquals(1899, previousDay.getYear(), "Year should be 1899 when subtracting one day from 1900-01-01");
        assertEquals(12, previousDay.getMonth(), "Month should be December when subtracting one day from 1900-01-01");
        assertEquals(31, previousDay.getDayOfMonth(), "Day should be 31 when subtracting one day from 1900-01-01");
    }

    @Test
    public void testMonthYearRollover() {
        Date dec31 = new Date(2021, 12, 31);
        Date jan1 = dec31.plusDays(1);
        assertEquals(2022, jan1.getYear(), "Year should roll over to 2022 after December 31, 2021");
        assertEquals(1, jan1.getMonth(), "Month should be January after December 31, 2021");
        assertEquals(1, jan1.getDayOfMonth(), "Day should be 1 after December 31, 2021");
    }

    @Test
    public void testConsistencyAcrossConversions() {
        Date original = new Date(2021, 7, 15);
        int year = original.getYear();
        int month = original.getMonth();
        int day = original.getDayOfMonth();

        Date reconstructed = new Date(year, month, day);

        assertEquals(original, reconstructed, "Reconstructed date should equal original date");
        assertEquals(original.getEpochDay(), reconstructed.getEpochDay(), "Epoch days should be equal");
    }
}
