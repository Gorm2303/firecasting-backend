package dk.gormkrings.simulation.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DateBoundaryTest {
    @Test
    public void testFebruaryLengthInLeapAndNonLeapYears() {
        Date leapFebDate = new Date(2020, 2, 29);
        assertEquals(29, leapFebDate.lengthOfMonth(), "February 2020 should have 29 days");
        assertEquals(366, leapFebDate.lengthOfYear(), "Year 2020 should have 366 days");

        Date nonLeapFebDate = new Date(2019, 2, 28);
        assertEquals(28, nonLeapFebDate.lengthOfMonth(), "February 2019 should have 28 days");
        assertEquals(365, nonLeapFebDate.lengthOfYear(), "Year 2019 should have 365 days");
    }

    @Test
    public void testMonthAndYearBoundaryTransitions() {
        Date dec31 = new Date(2021, 12, 31);
        assertEquals(dec31.getEpochDay(), new Date(2021, 12, 31).getEpochDay(),
                "Epoch day for December 31, 2021 should be consistent");

        int nextYearStartEpoch = dec31.computeNextYearStart();
        Date nextYearStart = new Date(nextYearStartEpoch);
        assertEquals(2022, nextYearStart.getYear(), "Next year's start should be in 2022");
        assertEquals(1, nextYearStart.getMonth(), "Next year's start month should be January");
        assertEquals(1, nextYearStart.getDayOfMonth(), "Next year's start day should be 1");

        Date jan1 = new Date(2021, 1, 1);
        int nextMonthStartEpoch = jan1.computeNextMonthStart();
        Date nextMonthStart = new Date(nextMonthStartEpoch);
        assertEquals(2021, nextMonthStart.getYear(), "Year should remain 2021 for next month start from January");
        assertEquals(2, nextMonthStart.getMonth(), "Next month start should be February");
        assertEquals(1, nextMonthStart.getDayOfMonth(), "Next month start day should be 1");
    }

    @Test
    public void testWeekBoundaryComputations() {
        Date monday = new Date(1900, 1, 1);
        int expectedNextWeekStartEpoch = monday.plusDays(7).getEpochDay();
        assertEquals(expectedNextWeekStartEpoch, monday.computeNextWeekStart(),
                "For Monday, next week start should be 7 days later");

        int expectedWeekEndEpoch = monday.plusDays(6).getEpochDay();
        assertEquals(expectedWeekEndEpoch, monday.computeWeekEnd(),
                "For Monday, week end should be 6 days later");

        Date tuesday = new Date(1900, 1, 2);

        int expectedNextWeekStartForTuesday = tuesday.plusDays(6).getEpochDay();
        assertEquals(expectedNextWeekStartForTuesday, tuesday.computeNextWeekStart(),
                "For Tuesday, next week start should be 6 days later");

        int expectedWeekEndForTuesday = tuesday.plusDays(5).getEpochDay();
        assertEquals(expectedWeekEndForTuesday, tuesday.computeWeekEnd(),
                "For Tuesday, week end should be 5 days later");

        assertEquals(expectedWeekEndForTuesday, tuesday.computeNextWeekEnd(),
                "For Tuesday, next week end should match the computed week end");
    }

    @Test
    public void testMonthBoundaryComputations() {
        Date midMay = new Date(2021, 5, 15);
        int expectedNextMonthStart = new Date(2021, 6, 1).getEpochDay();
        assertEquals(expectedNextMonthStart, midMay.computeNextMonthStart(),
                "Next month start from May should be June 1, 2021");

        Date decDate = new Date(2021, 12, 15);
        int expectedNextMonthStartDec = new Date(2022, 1, 1).getEpochDay();
        assertEquals(expectedNextMonthStartDec, decDate.computeNextMonthStart(),
                "Next month start from December should be January 1 of the next year");

        int expectedMonthEndMay = new Date(2021, 5, 31).getEpochDay();
        assertEquals(expectedMonthEndMay, midMay.computeMonthEnd(),
                "Month end for May 2021 should be May 31, 2021");

        Date may10 = new Date(2021, 5, 10);
        assertEquals(expectedMonthEndMay, may10.computeNextMonthEnd(),
                "For a non-month-end date, computeNextMonthEnd should return current month's end");

        Date mayEnd = new Date(2021, 5, 31);
        int expectedNextMonthEndForMayEnd = new Date(2021, 6, 30).getEpochDay();
        assertEquals(expectedNextMonthEndForMayEnd, mayEnd.computeNextMonthEnd(),
                "For a month-end date, computeNextMonthEnd should return next month's end");
    }

    @Test
    public void testYearBoundaryComputations() {
        Date someDay2021 = new Date(2021, 7, 15);
        int expectedNextYearStart = new Date(2022, 1, 1).getEpochDay();
        assertEquals(expectedNextYearStart, someDay2021.computeNextYearStart(),
                "Next year start for 2021 should be January 1, 2022");

        int expectedYearEnd2021 = new Date(2021, 12, 31).getEpochDay();
        assertEquals(expectedYearEnd2021, someDay2021.computeYearEnd(),
                "Year end for 2021 should be December 31, 2021");

        assertEquals(expectedYearEnd2021, someDay2021.computeNextYearEnd(),
                "For a non-year-end date, computeNextYearEnd should return December 31 of the same year");

        Date dec31 = new Date(2021, 12, 31);
        int expectedNextYearEnd = new Date(2022, 12, 31).getEpochDay();
        assertEquals(expectedNextYearEnd, dec31.computeNextYearEnd(),
                "For a year-end date, computeNextYearEnd should return December 31 of the following year");
    }
}
