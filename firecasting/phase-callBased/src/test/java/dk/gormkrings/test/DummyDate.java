package dk.gormkrings.test;

import dk.gormkrings.data.IDate;

public class DummyDate implements IDate {
    private final int epochDay;

    public DummyDate(int epochDay) {
        this.epochDay = epochDay;
    }

    @Override
    public int getEpochDay() {
        return epochDay;
    }

    @Override
    public IDate plusDays(long days) {
        return new DummyDate(epochDay + (int) days);
    }

    // For testing, you can return dummy values for the following methods:
    @Override
    public IDate plusMonths(int months) {
        // For simplicity, assume 30 days per month.
        return plusDays(months * 30L);
    }

    @Override
    public long daysUntil(IDate other) {
        return 0;
    }

    @Override
    public int getDayOfMonth() {
        return 0;
    }

    @Override
    public int getMonth() {
        return 0;
    }

    @Override
    public int getYear() {
        return 0;
    }

    @Override
    public int getDayOfYear() {
        return 0;
    }

    @Override
    public int getDayOfWeek() {
        return 0;
    }

    @Override
    public int lengthOfMonth() {
        return 0;
    }

    @Override
    public int lengthOfYear() {
        return 0;
    }

    @Override
    public int computeNextWeekStart() {
        return 0;
    }

    @Override
    public int computeNextWeekEnd() {
        return 0;
    }

    @Override
    public int computeNextMonthStart() {
        return 0;
    }

    @Override
    public int computeNextMonthEnd() {
        return 0;
    }

    @Override
    public int computeNextYearStart() {
        return 0;
    }

    @Override
    public int computeNextYearEnd() {
        return 0;
    }

    @Override
    public int computeMonthEnd() {
        return 0;
    }

    @Override
    public int computeWeekEnd() {
        return 0;
    }

    @Override
    public int computeYearEnd() {
        return 0;
    }

    // Other methods can be left unimplemented or return dummy values.
}
