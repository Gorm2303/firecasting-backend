package dk.gormkrings.util;

import dk.gormkrings.data.LiveData;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Util {
    public static boolean debug = false;

    public static String formatNumber(double number) {
        if (!debug) return "";
        String string;
        if (number > 100) {
            string = String.format("%.0f", number);
        } else if (number <= 100 && number >= 0.1) {
            string = String.format("%.2f", number);
        } else {
            string = String.format("%.4f", number);
        }
        return string;
    }

    public static String getPrettyDate(Date startDate, LiveData data) {
        if (!debug) return "";
        long days = data.getSessionDuration();
        Date currentDate = startDate.plusDays(data.getSessionDuration() - 1);
        int years = (currentDate.getYear()-startDate.getYear());
        int months = currentDate.getMonth() + years * 12;
        String formattedDate = "";
        formattedDate = (" - Day " + days + " - Month " + months + " - Year " + (years+1) + " - " + currentDate);
        return formattedDate;
    }

    public static String formatToString(String label, Object value) {
        return " - " + label + " " + value;
    }

    public static String formatField(String label, double value) {
        return Util.formatToString(label, formatNumber(value));
    }

    public static String formatField(String label, long value) {
        return Util.formatToString(label, value);
    }

    public static void debugLog(String string) {
        if (!debug) return;
        log.debug(string);
    }
}
