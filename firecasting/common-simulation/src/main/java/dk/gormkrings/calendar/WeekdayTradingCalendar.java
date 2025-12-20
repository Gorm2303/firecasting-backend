package dk.gormkrings.calendar;

import dk.gormkrings.data.IDate;

/**
 * Default trading calendar: Monday-Friday are trading days.
 */
public class WeekdayTradingCalendar implements TradingCalendar {

    @Override
    public boolean isTradingDay(IDate date) {
        if (date == null) return false;
        // Date.getDayOfWeek(): 0=Monday, ..., 6=Sunday.
        return date.getDayOfWeek() < 5;
    }
}
