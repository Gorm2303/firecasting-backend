package dk.gormkrings.data;

public interface IDate extends ITimeUnit {
    int getEpochDay();
    IDate plusDays(long days);
    IDate plusMonths(int months);
    long daysUntil(IDate other);

    int getDayOfMonth();
    int getMonth();
    int getYear();
    int getDayOfYear();
    int getDayOfWeek();

    int lengthOfMonth();
    int lengthOfYear();

    int computeNextWeekStart();
    int computeNextWeekEnd();
    int computeNextMonthStart();
    int computeNextMonthEnd();
    int computeNextYearStart();
    int computeNextYearEnd();

    int computeMonthEnd();
    int computeWeekEnd();
    int computeYearEnd();

    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();

    @Override
    String toString();
}
