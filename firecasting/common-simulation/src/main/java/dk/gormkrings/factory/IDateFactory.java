package dk.gormkrings.factory;

import dk.gormkrings.data.IDate;

public interface IDateFactory {
    IDate fromEpochDay(int epochDay);
    IDate dateOf(int year, int month, int day);
}
