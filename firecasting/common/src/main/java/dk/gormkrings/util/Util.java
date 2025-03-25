package dk.gormkrings.util;

import java.text.DecimalFormat;

public class Util {
    public static String formatNumber(double number) {
        String pattern;
        if (number > 100) {
            pattern = "0";
        } else if (number <= 100 && number >= 0.1) {
            pattern = "0.00";
        } else {
            pattern = "0.0000";
        }
        DecimalFormat df = new DecimalFormat(pattern);
        return df.format(number);
    }
}
