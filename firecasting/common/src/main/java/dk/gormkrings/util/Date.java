package dk.gormkrings.util;

import lombok.Getter;

@Getter
public final class Date {
    // Number of days since the epoch (1900-01-01)
    private final int epochDay;

    public Date(int epochDay) {
        this.epochDay = epochDay;
    }

    public static Date of(int year, int month, int day) {
        // Adjust for months January and February.
        if (month <= 2) {
            year--;
            month += 12;
        }
        // Compute Julian Day Number (JDN) for the given date.
        int a = year / 100;
        int b = 2 - a + a / 4;
        int jdn = (int)(365.25 * (year + 4716))
                + (int)(30.6001 * (month + 1))
                + day + b - 1524;
        // Epoch for 1900-01-01 in JDN is 2415021.
        int epochDay = jdn - 2415021;
        return new Date(epochDay);
    }

    public Date plusDays(long days) {
        return new Date(this.epochDay + (int) days);
    }

    public Date plusMonths(int months) {
        int[] ymd = toYMD();
        int year = ymd[0];
        int month = ymd[1];
        int day = ymd[2];

        int totalMonths = (month - 1) + months;
        int targetYear = year + totalMonths / 12;
        int targetMonth = (totalMonths % 12) + 1;
        int maxDay = getLengthOfMonth(targetYear, targetMonth);
        int targetDay = Math.min(day, maxDay);
        return Date.of(targetYear, targetMonth, targetDay);
    }

    private static int getLengthOfMonth(int year, int month) {
        switch (month) {
            case 2:
                return isLeapYearStatic(year) ? 29 : 28;
            case 4: case 6: case 9: case 11:
                return 30;
            default:
                return 31;
        }
    }

    private static boolean isLeapYearStatic(int year) {
        return ((year % 4 == 0) && ((year % 100 != 0) || (year % 400 == 0)));
    }

    public int getDayOfMonth() {
        int[] ymd = toYMD();
        return ymd[2];
    }

    public int getMonth() {
        int[] ymd = toYMD();
        return ymd[1];
    }

    public int getYear() {
        int[] ymd = toYMD();
        return ymd[0];
    }

    public int getDayOfYear() {
        Date firstDay = Date.of(getYear(), 1, 1);
        return this.epochDay - firstDay.epochDay + 1;
    }

    public int lengthOfMonth() {
        int month = getMonth();
        int year = getYear();
        switch (month) {
            case 2:
                return isLeapYear(year) ? 29 : 28;
            case 4: case 6: case 9: case 11:
                return 30;
            default:
                return 31;
        }
    }

    public int lengthOfYear() {
        return isLeapYear(getYear()) ? 366 : 365;
    }

    private boolean isLeapYear(int year) {
        return ((year % 4 == 0) && ((year % 100 != 0) || (year % 400 == 0)));
    }

    private int[] toYMD() {
        int j = epochDay + 2415021;
        int a = j + 32044;
        int b = (4 * a + 3) / 146097;
        int c = a - (146097 * b) / 4;
        int d = (4 * c + 3) / 1461;
        int e = c - (1461 * d) / 4;
        int m = (5 * e + 2) / 153;
        int day = e - (153 * m + 2) / 5 + 1;
        int month = m + 3 - 12 * (m / 10);
        int year = 100 * b + d - 4800 + (m / 10);
        return new int[]{year, month, day};
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof Date) {
            return this.epochDay == ((Date)obj).epochDay;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(epochDay);
    }

    @Override
    public String toString() {
        return String.format("%04d-%02d-%02d", getYear(), getMonth(), getDayOfMonth());
    }

    public long daysUntil(Date other) {
        return other.epochDay - this.epochDay;
    }


    public int computeNextMonthStart() {
        int year = this.getYear();
        int month = this.getMonth();
        if (month == 12) {
            return Date.of(year + 1, 1, 1).getEpochDay();
        } else {
            return Date.of(year, month + 1, 1).getEpochDay();
        }
    }

    public int computeMonthEnd() {
        int year = this.getYear();
        int month = this.getMonth();
        int day = this.lengthOfMonth();
        return Date.of(year, month, day).getEpochDay();
    }

    public int computeNextYearStart() {
        int year = this.getYear();
        return Date.of(year + 1, 1, 1).getEpochDay();
    }

    public int computeYearEnd() {
        int year = this.getYear();
        return Date.of(year, 12, 31).getEpochDay();
    }

}
