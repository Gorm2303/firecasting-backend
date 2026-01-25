package dk.gormkrings.simulation.util;

import dk.gormkrings.result.IRunResult;
import dk.gormkrings.result.ISnapshot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class CsvExporter {

    public static final String HEADER = "phase, day, month, year, date, capital, deposited, deposit, passive, returned, return, withdrawn, withdraw, taxed, tax, fees, fee, inflation, nettotal, net" +
        ", y-return, y-withdraw, y-tax, y-net";

    public static List<String> headers() {
    return java.util.Arrays.stream(HEADER.split(","))
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .toList();
    }

    public static File exportResultsToCsv(List<IRunResult> results, String fileName) throws IOException {
        File csvFile = new File(fileName);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))) {
        writer.write(HEADER);
            writer.newLine();

            ISnapshot previousSnapshot = null;
            for (IRunResult result : results) {
                for (ISnapshot snapshot : result.getSnapshots()) {
                    writer.write(snapshot.toCsvRow());
                    writer.write(addYearlyValues(snapshot, Objects.requireNonNullElse(previousSnapshot, snapshot)));
                    writer.newLine();
                    previousSnapshot = snapshot;
                }
                previousSnapshot = null;
            }
        }
        return csvFile;
    }

    private static String addYearlyValues(ISnapshot snapshot, ISnapshot previousSnapshot) {
        double yearReturn = snapshot.getState().getReturned() - previousSnapshot.getState().getReturned();
        double yearWithdraw = snapshot.getState().getWithdrawn() - previousSnapshot.getState().getWithdrawn();
        double yearTax = snapshot.getState().getTax() - previousSnapshot.getState().getTax();
        double yearNet = snapshot.getState().getNet() - previousSnapshot.getState().getNet();

        return  "," +
                Formatter.numberToString(yearReturn) + "," +
                Formatter.numberToString(yearWithdraw) + "," +
                Formatter.numberToString(yearTax) + "," +
                Formatter.numberToString(yearNet);
    }
}

