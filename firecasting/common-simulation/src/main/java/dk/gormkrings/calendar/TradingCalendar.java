package dk.gormkrings.calendar;

import dk.gormkrings.data.IDate;

/**
 * Determines whether a given date should be treated as a trading day.
 *
 * <p>Used to decide when DAILY returns should be applied. MONTHLY returns are applied on
 * calendar month-end and do not use this calendar.</p>
 */
public interface TradingCalendar {
    boolean isTradingDay(IDate date);
}
