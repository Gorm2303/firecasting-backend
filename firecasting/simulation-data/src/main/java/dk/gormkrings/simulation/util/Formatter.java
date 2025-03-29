package dk.gormkrings.simulation.util;

import dk.gormkrings.data.IDate;
import dk.gormkrings.data.ILiveData;
import dk.gormkrings.factory.IDateFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;

@Slf4j
public class Formatter {
    public static boolean debug = false;
    private static IDateFactory dateFactory;

    public Formatter(IDateFactory dateFactory) {
        Formatter.dateFactory = dateFactory;
    }

    public static String formatNumber(double number) {
        String string;
        if (number > 100) {
            string = String.format(Locale.US,"%.0f", number);
        } else if (number <= 100 && number >= 0.1) {
            string = String.format(Locale.US,"%.2f", number);
        } else {
            string = String.format(Locale.US,"%.4f", number);
        }
        return string;
    }

    public static String getPrettyDate(ILiveData data) {
        long session = data.getSessionDuration();
        long alive = data.getTotalDurationAlive();
        IDate startDate = dateFactory.fromEpochDay((int) data.getStartTime());
        IDate currentDate = startDate.plusDays(alive - 1);
        IDate sessionDate = startDate.plusDays(session - 1);
        int years = (sessionDate.getYear()-startDate.getYear());
        int months = sessionDate.getMonth() + years * 12;
        String formattedDate = "";
        formattedDate = (" - Day " + session + " - Month " + months + " - Year " + (years+1) + " - " + currentDate);
        return formattedDate;
    }

    public static String formatToString(String label, Object value) {
        return " - " + label + " " + value;
    }

    public static String formatField(String label, double value) {
        return Formatter.formatToString(label, formatNumber(value));
    }

    public static String formatField(String label, long value) {
        return Formatter.formatToString(label, value);
    }
}
