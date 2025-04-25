package dk.gormkrings.simulation.factory;

import dk.gormkrings.data.IDate;
import dk.gormkrings.factory.IDateFactory;
import dk.gormkrings.simulation.data.Date;
import org.springframework.stereotype.Component;

@Component
public class DefaultDateFactory implements IDateFactory {
    @Override
    public IDate fromEpochDay(int epochDay) {
        return new Date(epochDay);
    }
    @Override
    public IDate dateOf(int year, int month, int day) {
        return new Date(year, month, day);
    }

    @Override
    public IDate dateOf(IDate startDateOrNull) {
        if (startDateOrNull != null) {
            return startDateOrNull;
        }
        java.time.LocalDate today = java.time.LocalDate.now();
        return dateOf(
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth()
        );
    }

}
